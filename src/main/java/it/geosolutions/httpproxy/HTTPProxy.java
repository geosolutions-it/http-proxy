/*
 *  Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.httpproxy;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.ContentBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTPProxy class.
 *
 * @author Alessio Fabiani at alessio.fabiani@geo-solutions.it
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class HTTPProxy extends HttpServlet {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = -4770692886388850680L;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HTTPProxy.class);


    /**
     * An Apache commons HTTP client backed by a multithreaded connection manager that allows to reuse connections to the backing server and to limit
     * the max number of concurrent connections.
     */
    private PoolingHttpClientConnectionManager connectionManager;

    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    /**
     * An HTTP "user-agent", containing an HTTP state and one or more HTTP connections, to which HTTP methods can be applied.
     */
    private CloseableHttpClient httpClient;


    /**
     * The proxy configuration.
     */
    private ProxyConfig proxyConfig;

    /**
     * The proxy collbacks to provide checks.
     */
    private List<ProxyCallback> callbacks;

    private BasicCredentialsProvider credsProvider = null;

    /**
     * Initialize the <code>ProxyServlet</code>
     *
     * @param servletConfig The Servlet configuration passed in by the servlet conatiner
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        ServletContext context = getServletContext();
        String proxyPropPath = context.getInitParameter("proxyPropPath");

        proxyConfig = new ProxyConfig(getServletContext(), proxyPropPath);

        connectionManager = new PoolingHttpClientConnectionManager();

        connectionManager.setMaxTotal(6);
        connectionManager.setDefaultMaxPerRoute(6);

        httpClient = createHttpClient();

        // //////////////////////////////////////////
        // Setup the callbacks (in the future this
        // will be a pluggable lookup).
        // //////////////////////////////////////////

        callbacks = new ArrayList<>();
        callbacks.add(new MimeTypeChecker(proxyConfig));
        callbacks.add(new HostNameChecker(proxyConfig));
        callbacks.add(new RequestTypeChecker(proxyConfig));
        callbacks.add(new MethodsChecker(proxyConfig));
        callbacks.add(new HostChecker(proxyConfig));
    }

    /**
     * Creates the HttpClient
     * @return HttpClient
     */
    public CloseableHttpClient createHttpClient() {
        if (httpClient != null)
            return httpClient;

        final HttpHost httpHost = getHost("http.proxyHost", "http.proxyPort");
        LOGGER.debug("HTTP proxy host: {}", httpHost);
        final HttpHost httpsHost = getHost("https.proxyHost", "https.proxyPort");
        LOGGER.debug("HTTPS proxy host: {}", httpsHost);

        HttpRoutePlanner routePlanner = getRoutePlanner(httpHost, httpsHost);

        clientBuilder.setRoutePlanner(routePlanner);
        clientBuilder.useSystemProperties();
        clientBuilder.setConnectionManager(connectionManager);

        LOGGER.info("HTTP Client created");
        return clientBuilder.build();
    }

    private HttpHost getHost(String proxyHostKey, String proxyPortKey) {
        HttpHost httpHost = null;
        String proxyHost = System.getProperty(proxyHostKey);
        if (proxyHost != null && !proxyHost.isEmpty()) {
            int proxyPort = 80;
            proxyPort = (System.getProperty(proxyPortKey) != null ?
                    Integer.parseInt(System.getProperty(proxyPortKey)) : proxyPort);
            httpHost = new HttpHost(proxyHost, proxyPort);
        }
        return httpHost;
    }

    /**
     * Returns the HttpRoutePlanner based on the target host http scheme
     * @param httpHost
     * @param httpsHost
     * @return HttpRoutePlanner
     */
    private HttpRoutePlanner getRoutePlanner(final HttpHost httpHost, final HttpHost httpsHost) {
        return new HttpRoutePlanner() {
            public HttpRoute determineRoute(
                    HttpHost target,
                    HttpContext context) throws HttpException {
                LOGGER.info("HTTP proxy target host: {}", target);
                if (isNonProxyHost(target.getHostName())) {
                    LOGGER.info("Returning direct route");
                    // Return direct route
                    return new HttpRoute(target);
                } else {
                    // Return the proxy route
                    LOGGER.info("Returning proxy route");

                    return getProxyRoute(target);
                }
            }

            private HttpRoute getProxyRoute(HttpHost target) {
                if (target.getSchemeName().equals("http")) {
                    LOGGER.debug("Setting http scheme");
                    if (httpHost == null)
                        return new HttpRoute(target);
                    else
                        return new HttpRoute(target, null, httpHost,
                                false);
                } else {
                    LOGGER.debug("Setting https scheme");
                    if (httpsHost == null)
                        return new HttpRoute(target, null, true);
                    else
                        return new HttpRoute(target, null, httpsHost,
                                true);
                }
            }
        };
    }

    private boolean isNonProxyHost(String host) {
        boolean isNonProxyHost = false;
        String nonProxyHostProp = System.getProperty("http.nonProxyHosts");
        if (nonProxyHostProp != null) {
            if (nonProxyHostProp.startsWith("\"")) {
                nonProxyHostProp = nonProxyHostProp.substring(1);
            }
            if (nonProxyHostProp.endsWith("\"")) {
                nonProxyHostProp = nonProxyHostProp.substring(0, nonProxyHostProp.length() - 1);
            }
            LOGGER.info("http.nonProxyHosts value: {}", nonProxyHostProp);
            for (String token : nonProxyHostProp.split("\\|")) {
                String str = token.trim();
                str = str.replace("*", "[\\w-]*");
                Pattern pattern = Pattern.compile(str);
                Matcher matcher = pattern.matcher(host);
                if (matcher.matches()) {
                    LOGGER.info("Non proxy host matched for: {}", str);
                    isNonProxyHost = true;
                    break;
                }
            }
        }
        return isNonProxyHost;
    }

    /**
     * @param request
     * @param response
     * @throws IOException
     */
    void onInit(HttpServletRequest request, HttpServletResponse response, URL url)
            throws IOException {
        for (ProxyCallback callback : callbacks) {
            callback.onRequest(request, response, url);
        }
    }

    /**
     * @param method
     * @throws IOException
     */
    void onRemoteResponse(HttpUriRequestBase method) throws IOException {
        for (ProxyCallback callback : callbacks) {
            callback.onRemoteResponse(method);
        }
    }

    /**
     * @throws IOException
     */
    void onFinish() throws IOException {
        for (ProxyCallback callback : callbacks) {
            callback.onFinish();
        }
    }

    /**
     * Performs an HTTP GET request
     *
     * @param httpServletRequest  The {@link HttpServletRequest} object passed in by the servlet engine representing the client request to be proxied
     * @param httpServletResponse The {@link HttpServletResponse} object by which we can send a proxied response to the client
     */
    @Override
    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        try {

            URL url = null;
            String user = null;
            String password = null;

            Set<Map.Entry<String, String[]>> entrySet = httpServletRequest.getParameterMap().entrySet();

            for (Map.Entry<String, String[]> header : entrySet) {
                String key = header.getKey();
                String value = header.getValue()[0];

                if ("user".equals(key)) {
                    user = value;
                } else if ("password".equals(key)) {
                    password = value;
                } else if ("url".equals(key)) {
                    url = Utils.buildURL(value);
                }
            }

            if (url != null) {

                onInit(httpServletRequest, httpServletResponse, url);

                // //////////////////////////////
                // Create a GET request
                // //////////////////////////////

                HttpGet getMethodProxyRequest = getGetMethod(url);

                // //////////////////////////////
                // Forward the request headers
                // //////////////////////////////

                setProxyRequestHeaders(url, httpServletRequest, getMethodProxyRequest);

                // //////////////////////////////
                // Execute the proxy request
                // //////////////////////////////

                this.executeProxyRequest(getMethodProxyRequest, httpServletRequest,
                        httpServletResponse, user, password);
            }

        } catch (HttpErrorException ex) {
            httpServletResponse.sendError(ex.getCode(), ex.getMessage());
        } finally {
            onFinish();
        }
    }

    protected HttpGet getGetMethod(URL url) {
        return new HttpGet(url.toExternalForm());
    }

    /**
     * Performs an HTTP POST request
     *
     * @param httpServletRequest  The {@link HttpServletRequest} object passed in by the servlet engine representing the client request to be proxied
     * @param httpServletResponse The {@link HttpServletResponse} object by which we can send a proxied response to the client
     */
    @Override
    public void doPost(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse) throws IOException, ServletException {
        try {

            URL url = null;
            String user = null;
            String password = null;
            //Parse the queryString to not read the request body calling getParameter from httpServletRequest
            // so the method can simply forward the request body
            Map<String, String> pars = splitQuery(httpServletRequest.getQueryString());

            for (String key : pars.keySet()) {

                String value = pars.get(key);

                if ("user".equals(key)) {
                    user = value;
                } else if ("password".equals(key)) {
                    password = value;
                } else if ("url".equals(key)) {
                    url = Utils.buildURL(value);
                }
            }

            if (url != null) {

                onInit(httpServletRequest, httpServletResponse, url);

                // /////////////////////////////////
                // Create a standard POST request
                // /////////////////////////////////

                HttpPost postMethodProxyRequest = getPostMethod(url);

                // //////////////////////////////
                // Forward the request headers
                // //////////////////////////////

                setProxyRequestHeaders(url, httpServletRequest, postMethodProxyRequest);


                // //////////////////////////////////////////////////
                // Check if this is a mulitpart (file upload) POST
                // //////////////////////////////////////////////////

                if (JakartaServletFileUpload.isMultipartContent(httpServletRequest)) {
                    this.handleMultipart(postMethodProxyRequest, httpServletRequest);
                } else {
                    this.handleStandard(postMethodProxyRequest, httpServletRequest);
                }

                // ///////////////////////////////
                // Execute the proxy request
                // ///////////////////////////////
                this.executeProxyRequest(postMethodProxyRequest, httpServletRequest,
                        httpServletResponse, user, password);

            }

        } catch (HttpErrorException ex) {
            httpServletResponse.sendError(ex.getCode(), ex.getMessage());
        } finally {
            onFinish();
        }
    }

    protected HttpPost getPostMethod(URL url) {
        return new HttpPost(url.toExternalForm());
    }

    /**
     * Performs an HTTP PUT request
     *
     * @param httpServletRequest  The {@link HttpServletRequest} object passed in by the servlet engine representing the client request to be proxied
     * @param httpServletResponse The {@link HttpServletResponse} object by which we can send a proxied response to the client
     */
    @Override
    public void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        try {

            URL url = null;
            String user = null;
            String password = null;
            //Parse the queryString to not read the request body calling getParameter from httpServletRequest
            // so the method can simply forward the request body
            Map<String, String> pars = splitQuery(httpServletRequest.getQueryString());

            for (String key : pars.keySet()) {

                String value = pars.get(key);

                if ("user".equals(key)) {
                    user = value;
                } else if ("password".equals(key)) {
                    password = value;
                } else if ("url".equals(key)) {
                    url = Utils.buildURL(value);
                }
            }
            if (url != null) {

                onInit(httpServletRequest, httpServletResponse, url);

                // ////////////////////////////////
                // Create a standard PUT request
                // ////////////////////////////////

                HttpPut putMethodProxyRequest = new HttpPut(url.toExternalForm());

                // //////////////////////////////
                // Forward the request headers
                // //////////////////////////////

                setProxyRequestHeaders(url, httpServletRequest, putMethodProxyRequest);

                // //////////////////////////////////////////////////
                // Check if this is a mulitpart (file upload) PUT
                // //////////////////////////////////////////////////

                if (JakartaServletFileUpload.isMultipartContent(httpServletRequest)) {
                    this.handleMultipart(putMethodProxyRequest, httpServletRequest);
                } else {
                    this.handleStandard(putMethodProxyRequest, httpServletRequest);
                }

                // ////////////////////////////////
                // Execute the proxy request
                // ////////////////////////////////

                this.executeProxyRequest(putMethodProxyRequest, httpServletRequest,
                        httpServletResponse, user, password);

            }

        } catch (HttpErrorException ex) {
            httpServletResponse.sendError(ex.getCode(), ex.getMessage());
        } finally {
            onFinish();
        }

    }

    /**
     * Performs an HTTP DELETE request
     *
     * @param httpServletRequest  The {@link HttpServletRequest} object passed in by the servlet engine representing the client request to be proxied
     * @param httpServletResponse The {@link HttpServletResponse} object by which we can send a proxied response to the client
     */
    @Override
    public void doDelete(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse) throws IOException, ServletException {

        try {
            URL url = null;
            String user = null;
            String password = null;

            //Parse the queryString to not read the request body calling getParameter from httpServletRequest
            Map<String, String> pars = splitQuery(httpServletRequest.getQueryString());

            for (String key : pars.keySet()) {

                String value = pars.get(key);

                if ("user".equals(key)) {
                    user = value;
                } else if ("password".equals(key)) {
                    password = value;
                } else if ("url".equals(key)) {
                    url = Utils.buildURL(value);
                }
            }

            if (url != null) {

                onInit(httpServletRequest, httpServletResponse, url);

                // ////////////////////////////////
                // Create a standard DELETE request
                // ////////////////////////////////

                HttpDelete deleteMethodProxyRequest = new HttpDelete(url.toExternalForm());

                // //////////////////////////////
                // Forward the request headers
                // //////////////////////////////

                setProxyRequestHeaders(url, httpServletRequest, deleteMethodProxyRequest);

                // ////////////////////////////////
                // Execute the proxy request
                // ////////////////////////////////

                this.executeProxyRequest(deleteMethodProxyRequest, httpServletRequest,
                        httpServletResponse, user, password);

            }

        } catch (HttpErrorException ex) {
            httpServletResponse.sendError(ex.getCode(), ex.getMessage());
        } finally {
            onFinish();
        }
    }

    /**
     * Sets up the given {@link HttpUriRequestBase} to send the same multipart POST data as was sent in the given {@link HttpServletRequest}
     *
     * @param methodProxyRequest The {@link HttpUriRequestBase} that we are configuring to send a multipart POST request
     * @param httpServletRequest     The {@link HttpServletRequest} that contains the mutlipart POST data to be sent via the {@link HttpUriRequestBase}
     * @throws IOException
     */
    private void handleMultipart(HttpUriRequestBase methodProxyRequest,
                                 HttpServletRequest httpServletRequest) throws ServletException, IOException {

        // ////////////////////////////////////////////
        // Create a factory for disk-based file items
        // ////////////////////////////////////////////

        DiskFileItemFactory diskFileItemFactory = DiskFileItemFactory.builder()
                .setBufferSize(this.getMaxFileUploadSize())
                .setPath(Utils.DEFAULT_FILE_UPLOAD_TEMP_DIRECTORY.toPath())
                .get();

        // //////////////////////////////////
        // Create a new file upload handler
        // //////////////////////////////////

        JakartaServletFileUpload servletFileUpload = new JakartaServletFileUpload(diskFileItemFactory);

        // //////////////////////////
        // Parse the request
        // //////////////////////////

        try {

            // /////////////////////////////////////
            // Get the multipart items as a list
            // /////////////////////////////////////

            List<FileItem> listFileItems = servletFileUpload
                    .parseRequest(httpServletRequest);

            // /////////////////////////////////////////
            // Iterate the multipart items list
            // /////////////////////////////////////////

            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

            for (FileItem fileItemCurrent : listFileItems) {


                // If the current Item is form field
                if (fileItemCurrent.isFormField()) {

                    multipartEntityBuilder = multipartEntityBuilder.addTextBody(fileItemCurrent.getFieldName(), fileItemCurrent.getString());

                } else {
                    // If the current Item is file upload
                    multipartEntityBuilder = multipartEntityBuilder.addPart("file", (ContentBody) fileItemCurrent);
                }
            }

            HttpEntity entity = multipartEntityBuilder.build();

            if (methodProxyRequest instanceof HttpPost httpPost) {
                httpPost.setEntity(entity);
            } else if (methodProxyRequest instanceof HttpPut httpPut) {
                httpPut.setEntity(entity);
            }

            // ////////////////////////////////////////////////////////////////////////
            // The current content-type header (received from the client) IS of
            // type "multipart/form-data", but the content-type header also
            // contains the chunk boundary string of the chunks. Currently, this
            // header is using the boundary of the client request, since we
            // blindly copied all headers from the client request to the proxy
            // request. However, we are creating a new request with a new chunk
            // boundary string, so it is necessary that we re-set the
            // content-type string to reflect the new chunk boundary string
            // ////////////////////////////////////////////////////////////////////////

            String contentType = entity.getContentType();
            if (contentType != null) {
                methodProxyRequest.setHeader("Content-Type", contentType);
            }

        } catch (FileUploadException fileUploadException) {
            throw new ServletException(fileUploadException);
        }
    }

    /**
     * Sets up the given {@link PostMethod} to send the same standard POST data as was sent in the given {@link HttpServletRequest}
     *
     * @param postMethodProxyRequest The {@link PostMethod} that we are configuring to send a standard POST request
     * @param httpServletRequest     The {@link HttpServletRequest} that contains the POST data to be sent via the {@link PostMethod}
     * @throws IOException
     */
    private void handleStandard(HttpUriRequestBase methodProxyRequest,
                                HttpServletRequest httpServletRequest) throws IOException {
        String incomingCT = httpServletRequest.getContentType();
        ContentType contentType = incomingCT != null
                ? ContentType.parse(incomingCT)
                : ContentType.DEFAULT_BINARY;

        if (methodProxyRequest instanceof HttpPost httpPost) {
            httpPost.setEntity(new InputStreamEntity(httpServletRequest.getInputStream(), contentType));
        } else if (methodProxyRequest instanceof HttpPut httpPut) {
            httpPut.setEntity(new InputStreamEntity(httpServletRequest.getInputStream(), contentType));
        }
    }

    /**
     * Executes the {@link HttpMethod} passed in and sends the proxy response back to the client via the given {@link HttpServletResponse}
     *
     * @param httpMethodProxyRequest An object representing the proxy request to be made
     * @param httpServletResponse    An object by which we can send the proxied response back to the client
     * @param digest
     */
    private void executeProxyRequest(HttpUriRequestBase httpMethodProxyRequest,
                                     HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                     String user, String password) {

        if (user != null && password != null) {
            Credentials credentials = new UsernamePasswordCredentials(user, password.toCharArray());
            credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(null, -1), credentials);
            clientBuilder.setDefaultCredentialsProvider(credsProvider);
            httpClient = null;
            httpClient = createHttpClient();
        }

        try {
            httpClient.execute(httpMethodProxyRequest, response -> {

                onRemoteResponse(httpMethodProxyRequest);

                // ////////////////////////////////////////////////////////////////////////////////
                // Check if the proxy response is a redirect
                // The following code is adapted from
                // org.tigris.noodle.filters.CheckForRedirect
                // Hooray for open source software
                // ////////////////////////////////////////////////////////////////////////////////

                if (getStatusCode(response) >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
                        && getStatusCode(response) < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {

                    String stringStatusCode = Integer.toString(getStatusCode(response));
                    String stringLocation = httpMethodProxyRequest.getFirstHeader(
                            Utils.LOCATION_HEADER).getValue();

                    if (stringLocation == null) {
                        throw new IOException("Received status code: " + stringStatusCode
                                + " but no " + Utils.LOCATION_HEADER
                                + " header was found in the response");
                    }

                    // /////////////////////////////////////////////
                    // Modify the redirect to go to this proxy
                    // servlet rather that the proxied host
                    // /////////////////////////////////////////////

                    String redirectURL = httpServletRequest.getRequestURL() + "?url=" + URLEncoder.encode(stringLocation, StandardCharsets.UTF_8);
                    httpServletResponse.sendRedirect(redirectURL);
                    LOGGER.info("redirected to: {}", redirectURL);
                    return null;

                } else if (getStatusCode(response) == HttpServletResponse.SC_NOT_MODIFIED) {

                    // ///////////////////////////////////////////////////////////////
                    // 304 needs special handling. See:
                    // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
                    // We get a 304 whenever passed an 'If-Modified-Since'
                    // header and the data on disk has not changed; server
                    // responds w/ a 304 saying I'm not going to send the
                    // body because the file has not changed.
                    // ///////////////////////////////////////////////////////////////

                    httpServletResponse.setIntHeader(Utils.CONTENT_LENGTH_HEADER_NAME, 0);
                    httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return null;
                }

                // /////////////////////////////////////////////
                // Pass the response code back to the client
                // /////////////////////////////////////////////

                httpServletResponse.setStatus(getStatusCode(response));

                // /////////////////////////////////////////////
                // Pass response headers back to the client
                // /////////////////////////////////////////////

                var headerIt = response.headerIterator();
                while (headerIt.hasNext()) {
                    var header = headerIt.next();

                    // /////////////////////////
                    // Skip GZIP Responses
                    // /////////////////////////

                    if (header.getName().equalsIgnoreCase(Utils.HTTP_HEADER_ACCEPT_ENCODING)
                            && header.getValue().toLowerCase().contains("gzip"))
                        continue;
                    else if (header.getName().equalsIgnoreCase(Utils.HTTP_HEADER_CONTENT_ENCODING)
                            && header.getValue().toLowerCase().contains("gzip"))
                        continue;
                    else if (header.getName().equalsIgnoreCase(Utils.HTTP_HEADER_TRANSFER_ENCODING))
                        continue;
                    else
                        httpServletResponse.setHeader(header.getName(), header.getValue());
                }

                // ///////////////////////////////////
                // Send the content to the client
                // ///////////////////////////////////

                try (InputStream inputStreamServerResponse = response.getEntity().getContent()) {
                    if (inputStreamServerResponse != null) {
                        byte[] b = new byte[proxyConfig.getDefaultStreamByteSize()];

                        int read;
                        ServletOutputStream out = httpServletResponse.getOutputStream();
                        while ((read = inputStreamServerResponse.read(b)) > 0) {
                            out.write(b, 0, read);
                        }
                    }
                }

                return null;
            });
        } catch (Exception e) {
            LOGGER.error("Error executing HTTP method", e);
        }
    }

    int getStatusCode(ClassicHttpResponse response) {
        if (response != null) {
            return response.getCode();
        } else {
            return -1;
        }
    }

    /**
     * Retrieves all of the headers from the servlet request and sets them on the proxy request
     *
     * @param httpServletRequest     The request object representing the client's request to the servlet engine
     * @param httpMethodProxyRequest The request that we are about to send to the proxy host
     * @return ProxyInfo
     */
    private ProxyInfo setProxyRequestHeaders(URL url, HttpServletRequest httpServletRequest,
                                             HttpUriRequestBase httpMethodProxyRequest) {

        final String proxyHost = url.getHost();
        final int proxyPort = url.getPort();
        final String proxyPath = url.getPath();
        final ProxyInfo proxyInfo = new ProxyInfo(proxyHost, proxyPath, proxyPort);

        // ////////////////////////////////////////
        // Get an Enumeration of all the header
        // names sent by the client.
        // ////////////////////////////////////////

        // ////////////////////////////////////////
        // Load header whitelist/blacklist for
        // filtering forwarded request headers.
        // ////////////////////////////////////////

        Set<String> headerWhitelist = proxyConfig.getRequestHeaderWhitelist();
        Set<String> headerBlacklist = proxyConfig.getRequestHeaderBlacklist();

        for (String stringHeaderName : Collections.list(httpServletRequest.getHeaderNames())) {

            if (stringHeaderName.equalsIgnoreCase(Utils.CONTENT_LENGTH_HEADER_NAME))
                continue;

            // ////////////////////////////////////////
            // Apply header blacklist: always reject
            // ////////////////////////////////////////

            if (headerBlacklist != null && !headerBlacklist.isEmpty()) {
                if (headerBlacklist.contains(stringHeaderName.toLowerCase())) {
                    continue;
                }
            }

            // ////////////////////////////////////////
            // Apply header whitelist: if set, only
            // allow headers in the whitelist
            // ////////////////////////////////////////

            if (headerWhitelist != null && !headerWhitelist.isEmpty()) {
                if (!headerWhitelist.contains(stringHeaderName.toLowerCase())) {
                    continue;
                }
            }

            // ////////////////////////////////////////////////////////////////////////
            // As per the Java Servlet API 2.5 documentation:
            // Some headers, such as Accept-Language can be sent by clients
            // as several headers each with a different value rather than
            // sending the header as a comma separated list.
            // Thus, we get an Enumeration of the header values sent by the client
            // ////////////////////////////////////////////////////////////////////////

            for (String stringHeaderValue : Collections.list(httpServletRequest.getHeaders(stringHeaderName))) {

                // ////////////////////////////////////////////////////////////////
                // In case the proxy host is running multiple virtual servers,
                // rewrite the Host header to ensure that we get content from
                // the correct virtual server
                // ////////////////////////////////////////////////////////////////

                if (stringHeaderName.equalsIgnoreCase(Utils.HOST_HEADER_NAME)) {
                    stringHeaderValue = Utils.getProxyHostAndPort(proxyInfo);
                }

                // ////////////////////////
                // Skip GZIP Responses
                // ////////////////////////

                if (stringHeaderName.equalsIgnoreCase(Utils.HTTP_HEADER_ACCEPT_ENCODING)
                        && stringHeaderValue.toLowerCase().contains("gzip"))
                    continue;
                if (stringHeaderName.equalsIgnoreCase(Utils.HTTP_HEADER_CONTENT_ENCODING)
                        && stringHeaderValue.toLowerCase().contains("gzip"))
                    continue;
                if (stringHeaderName.equalsIgnoreCase(Utils.HTTP_HEADER_TRANSFER_ENCODING))
                    continue;

                // /////////////////////////////////////////////
                // Set the same header on the proxy request
                // /////////////////////////////////////////////

                httpMethodProxyRequest.setHeader(stringHeaderName, stringHeaderValue);

            }
        }

        return proxyInfo;
    }

    private Map<String, String> splitQuery(String query) {
        Map<String, String> query_pairs = new LinkedHashMap<>();

        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            query_pairs.put(
                URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
            );
        }
        return query_pairs;
    }

    /**
     * @return int the maximum file upload size.
     */
    public int getMaxFileUploadSize() {
        return Utils.DEFAULT_MAX_FILE_UPLOAD_SIZE;
    }

    /**
     * @return the client
     */
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * set the httpClient
     *
     * @param httpClient the client to set
     */
    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
