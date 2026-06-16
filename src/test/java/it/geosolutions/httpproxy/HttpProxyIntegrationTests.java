package it.geosolutions.httpproxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class HttpProxyIntegrationTests {

    private final int localPort = 8080;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HttpProxyIntegrationTests.class);

    @RegisterExtension
    static WireMockExtension wireMockRule = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension wireMockRule1 = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().dynamicPort())
            .build();

    static WireMockServer wireMockServer;

    static Server jettyServer = null;

    private static String proxyHost;
    private static String proxyPort;

    @BeforeAll
    public static void startHttpProxyServer() {

        try {
            // store system proxy config
            storeProxyConfig();

            wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
            wireMockServer.start();

            jettyServer = new Server();
            ServerConnector connector = new ServerConnector(jettyServer);
            connector.setPort(8080);
            jettyServer.addConnector(connector);

            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/http_proxy");
            webapp.setWar(new File("src/main/webapp").getAbsolutePath());
            webapp.setTempDirectory(new File("target/work"));
            jettyServer.setHandler(webapp);

            jettyServer.start();

        } catch (Exception e1) {
            LOGGER.error("Could not start HTTP Proxy Server: " + e1.getMessage(), e1);
            if (jettyServer != null) {
                try {
                    jettyServer.stop();
                } catch (Exception e2) {
                    LOGGER.error("Could not stop HTTP Proxy Server: " + e2.getMessage(), e2);
                }
            }
        }

    }

    /**
     * store the system proxy host and port values
     */
    private static void storeProxyConfig() {
        proxyHost = System.getProperty("http.proxyHost");
        proxyPort = System.getProperty("http.proxyPort");
    }

    /**
     * Validates that HTTP GET requests is correctly handled by the HTTP Proxy
     */
    @Test
    public void testGETUsingWireMock() throws IOException {

        wireMockRule.stubFor(
            get(urlEqualTo("/geostore/users"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody("<response>Some content</response>")));

        String url = "http://localhost:" + wireMockRule.getPort() + "/geostore/users";
        String proxyURL = "http://localhost:" + localPort + "/http_proxy/proxy?url=" + url;
        HttpGet httpGet = new HttpGet(proxyURL);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpResponse httpResponse = httpClient.execute(httpGet);
            Assertions.assertEquals(200, httpResponse.getCode());
            wireMockRule.verify(getRequestedFor(urlEqualTo("/geostore/users")));
        }
    }

    /**
     * Validates that HTTP POST request is correctly handled by the HTTP Proxy using text/xml content type
     */
    @Test
    public void testPOSTUsingWireMockXML() throws IOException {

        String requestBody = "<user><userId>5</userId><userName>Jane Doe</userName></user>";
        wireMockRule1.stubFor(
                post(urlEqualTo("/geostore/users/create"))
                        .withRequestBody(equalToXml(requestBody))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "text/xml")
                                        .withBody("<response>5</response>")));

        String url = "http://localhost:" + wireMockRule1.getPort() + "/geostore/users/create";
        String proxyURL = "http://localhost:" + localPort + "/http_proxy/proxy?url=" + url;
        HttpPost httpPost = new HttpPost(proxyURL);
        StringEntity stringEntity = new StringEntity(requestBody);
        httpPost.setEntity(stringEntity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpResponse httpResponse = httpClient.execute(httpPost);
            Assertions.assertEquals("text/xml", httpResponse.getFirstHeader("Content-Type").getValue());
            Assertions.assertEquals(201, httpResponse.getCode());
            wireMockRule1.verify(postRequestedFor(urlEqualTo("/geostore/users/create")));
        }
    }

    /**
     * Validates that HTTP POST request is correctly handled by the HTTP Proxy using application/json content type
     */
    @Test
    public void testPOSTUsingWireMockJSON() throws IOException {

        String requestBody = "{\n" +
                "  \"user\": {\n" +
                "    \"userId\": 5,\n" +
                "    \"userName\": \"Jane Doe\"\n" +
                "  }\n" +
                "}";
        wireMockRule1.stubFor(
                post(urlEqualTo("/geostore/users/create"))
                        .withRequestBody(equalToJson(requestBody))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\n" +
                                                "  \"response\": 5\n" +
                                                "}")));

        String url = "http://localhost:" + wireMockRule1.getPort() + "/geostore/users/create";
        String proxyURL = "http://localhost:" + localPort + "/http_proxy/proxy?url=" + url;
        HttpPost httpPost = new HttpPost(proxyURL);
        StringEntity stringEntity = new StringEntity(requestBody);
        httpPost.setEntity(stringEntity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpResponse httpResponse = httpClient.execute(httpPost);
            Assertions.assertEquals("application/json", httpResponse.getFirstHeader("Content-Type").getValue());
            Assertions.assertEquals(201, httpResponse.getCode());
            wireMockRule1.verify(postRequestedFor(urlEqualTo("/geostore/users/create")));
        }
    }

    /**
     * Validates that HTTP PUT request is correctly handled by the HTTP Proxy
     */
    @Test
    public void testPUTUsingWireMock() throws IOException {

        String requestBody = "<user><userId>5</userId><userName>John Doe</userName></user>";
        wireMockRule.stubFor(
            put(urlEqualTo("/geostore/users/5"))
               .withRequestBody(equalToXml(requestBody))
               .willReturn(
                   aResponse()
                       .withStatus(200)
                       .withHeader("Content-Type", "text/xml")
                       .withBody("<response>5</response>")));

        String url = "http://localhost:" + wireMockRule.getPort() + "/geostore/users/5";
        String proxyURL = "http://localhost:" + localPort + "/http_proxy/proxy?url=" + url;
        HttpPut httpPut = new HttpPut(proxyURL);
        StringEntity stringEntity = new StringEntity(requestBody);
        httpPut.setEntity(stringEntity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpResponse httpResponse = httpClient.execute(httpPut);
            Assertions.assertEquals(200, httpResponse.getCode());
            wireMockRule.verify(putRequestedFor(urlEqualTo("/geostore/users/5")));
        }
    }

    /**
     * Test the request for Empty (i.e. Null) proxy configuration
     */
    @Test
    public void testNullProxyConfig() throws IOException {

        // remove the proxy config
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");

        wireMockRule.stubFor(
            get(urlEqualTo("/geostore/resources"))
                .willReturn(
                    aResponse()
                      .withStatus(200)));

        String url = "http://localhost:" + wireMockRule.getPort() + "/geostore/resources";
        String proxyURL = "http://localhost:" + localPort + "/http_proxy/proxy?url=" + url;
        HttpGet httpGet = new HttpGet(proxyURL);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpResponse httpResponse = httpClient.execute(httpGet);
            Assertions.assertEquals(200, httpResponse.getCode());
            wireMockRule.verify(getRequestedFor(urlEqualTo("/geostore/resources")));
        }
    }

    /**
     * Validates that HTTP URL pot set to 80 if no port is specified
     */
    @Test
    public void testHttpDefaultPort() throws IOException {
        String urlStr = "http://localhost/geoserver/wms?service=wms%26version=1.3.0&request=GetCapabilities";
        URL url = Utils.buildURL(urlStr);
        Assertions.assertEquals(url.getPort(), Utils.DEFAULT_HTTP_PORT);
        Assertions.assertEquals(url.getProtocol(), "http");
        Assertions.assertEquals(url.getHost(), "localhost");
        Assertions.assertEquals(url.getFile(), "/geoserver/wms?service=wms%26version=1.3.0&request=GetCapabilities");
    }

    /**
     * Validates that HTTP URL pot set to 80 if no port is specified
     */
    @Test
    public void testHttpsDefaultPort() throws IOException {
        String urlStr = "https://georchestra.geo-solutions.it/geoserver/wms?service=WMS%26version=1.3.0&request=GetCapabilities";
        URL url = Utils.buildURL(urlStr);
        Assertions.assertEquals(url.getPort(), Utils.DEFAULT_HTTPS_PORT);
        Assertions.assertEquals(url.getProtocol(), "https");
        Assertions.assertEquals(url.getHost(), "georchestra.geo-solutions.it");
        Assertions.assertEquals(url.getFile(), "/geoserver/wms?service=WMS%26version=1.3.0&request=GetCapabilities");
    }


    @AfterAll
    public static void stopServer() {
        try {

            // restore system proxy config
            restoreProxyConfig();

            wireMockServer.stop();
            jettyServer.stop();
        } catch (Exception e) {
            LOGGER.error("Could not stop HTTP Proxy Server: " + e.getMessage(), e);
        }
    }

    /**
     * restore the system proxy config
     */
    private static void restoreProxyConfig() {
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
    }

}
