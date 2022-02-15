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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.LoggerFactory;

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
     * The maximum size for uploaded files in bytes. Default value is 5MB.
     */
    private int maxFileUploadSize = Utils.DEFAULT_MAX_FILE_UPLOAD_SIZE;

    /**
     * An Apache commons HTTP client backed by a multithreaded connection manager that allows to reuse connections to the backing server and to limit
     * the max number of concurrent connections.
     */
    private PoolingHttpClientConnectionManager connectionManager;

    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    /**
     * An HTTP "user-agent", containing an HTTP state and one or more HTTP connections, to which HTTP methods can be applied.
     */
    private HttpClient httpClient;


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

        callbacks = new ArrayList<ProxyCallback>();
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
    public HttpClient createHttpClient() {
        if (httpClient != null)
            return httpClient;

        final HttpHost httpHost = getHost("http.proxyHost", "http.proxyPort");
        LOGGER.debug("HTTP proxy host: " + httpHost);
        final HttpHost httpsHost = getHost("https.proxyHost", "https.proxyPort");
        LOGGER.debug("HTTPS proxy host: " + httpsHost);

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
                    HttpRequest request,
                    HttpContext context) {
                LOGGER.info("HTTP proxy target host: " + target);
                if (isNonProxyHost(target.getHostName())) {
                    LOGGER.info("Returning direct route");
                    // Return direct route
                    return new HttpRoute(target);
                } else {
                    // Return the proxy route
                    LOGGER.info("Returning proxy route");
                    if (target.getSchemeName().equals("http")) {
                        LOGGER.debug("Setting http scheme");
                        return new HttpRoute(target, null, httpHost,
                                false);
                    } else {
                        LOGGER.debug("Setting https scheme");
                        return new HttpRoute(target, null, httpsHost,
                                true);
                    }
                }
            }
        };
    }

    private Boolean isNonProxyHost(String host) {
        boolean isNonProxyHost = false;
        String nonProxyHostProp = System.getProperty("http.nonProxyHosts");
        if (nonProxyHostProp != null) {
            if (nonProxyHostProp.startsWith("\"")) {
                nonProxyHostProp = nonProxyHostProp.substring(1);
            }
            if (nonProxyHostProp.endsWith("\"")) {
                nonProxyHostProp = nonProxyHostProp.substring(0, nonProxyHostProp.length() - 1);
            }
            LOGGER.info("http.nonProxyHosts value: " + nonProxyHostProp);
            StringTokenizer tokenizer = new StringTokenizer(nonProxyHostProp, "|");
            while (tokenizer.hasMoreTokens()) {
                String str = tokenizer.nextToken().trim();
                str = str.replace("*", "[\\w-]*");
                Pattern pattern = Pattern.compile(str);
                Matcher matcher = pattern.matcher(host);
                if (matcher.matches()) {
                    LOGGER.info("Non proxy host matched for: " + str);
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
    void onRemoteResponse(HttpRequestBase method) throws IOException {
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
    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        try {

            URL url = null;
            String user = null, password = null;

            Set<?> entrySet = httpServletRequest.getParameterMap().entrySet();

            for (Object anEntrySet : entrySet) {
                Map.Entry header = (Map.Entry) anEntrySet;
                String key = (String) header.getKey();
                String value = ((String[]) header.getValue())[0];

                if ("user".equals(key)) {
                    user = value;
                } else if ("password".equals(key)) {
                    password = value;
                } else if ("url".equals(key)) {
                    url = new URL(value);
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
    public void doPost(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse) throws IOException, ServletException {
        try {

            URL url = null;
            String user = null, password = null;
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
                    url = new URL(value);
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

                if (ServletFileUpload.isMultipartContent(httpServletRequest)) {
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
    public void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        try {

            URL url = null;
            String user = null, password = null;
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
                    url = new URL(value);
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

                if (ServletFileUpload.isMultipartContent(httpServletRequest)) {
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
    public void doDelete(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse) throws IOException, ServletException {

        try {
            URL url = null;
            String user = null, password = null;

            //Parse the queryString to not read the request body calling getParameter from httpServletRequest
            Map<String, String> pars = splitQuery(httpServletRequest.getQueryString());

            for (String key : pars.keySet()) {

                String value = pars.get(key);

                if ("user".equals(key)) {
                    user = value;
                } else if ("password".equals(key)) {
                    password = value;
                } else if ("url".equals(key)) {
                    url = new URL(value);
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
     * Sets up the given {@link PostMethod} to send the same multipart POST data as was sent in the given {@link HttpServletRequest}
     *
     * @param postMethodProxyRequest The {@link PostMethod} that we are configuring to send a multipart POST request
     * @param httpServletRequest     The {@link HttpServletRequest} that contains the mutlipart POST data to be sent via the {@link PostMethod}
     */
    private void handleMultipart(HttpRequestBase methodProxyRequest,
                                 HttpServletRequest httpServletRequest) throws ServletException {

        // ////////////////////////////////////////////
        // Create a factory for disk-based file items
        // ////////////////////////////////////////////

        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();

        // /////////////////////////////
        // Set factory constraints
        // /////////////////////////////

        diskFileItemFactory.setSizeThreshold(this.getMaxFileUploadSize());
        diskFileItemFactory.setRepository(Utils.DEFAULT_FILE_UPLOAD_TEMP_DIRECTORY);

        // //////////////////////////////////
        // Create a new file upload handler
        // //////////////////////////////////

        ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);

        // //////////////////////////
        // Parse the request
        // //////////////////////////

        try {

            // /////////////////////////////////////
            // Get the multipart items as a list
            // /////////////////////////////////////

            List<FileItem> listFileItems = (List<FileItem>) servletFileUpload
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

            if (methodProxyRequest instanceof HttpPost) {
                ((HttpPost) methodProxyRequest).setEntity(entity);
            } else if (methodProxyRequest instanceof HttpPut) {
                ((HttpPut) methodProxyRequest).setEntity(entity);
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

            methodProxyRequest.setHeader(entity.getContentType());

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
    private void handleStandard(HttpRequestBase methodProxyRequest,
                                HttpServletRequest httpServletRequest) throws IOException {
        try {

            if (methodProxyRequest instanceof HttpPost) {
                ((HttpPost) methodProxyRequest).setEntity(new InputStreamEntity(httpServletRequest.getInputStream()));
            } else if (methodProxyRequest instanceof HttpPut) {
                ((HttpPut) methodProxyRequest).setEntity(new InputStreamEntity(httpServletRequest.getInputStream()));
            }

        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * Executes the {@link HttpMethod} passed in and sends the proxy response back to the client via the given {@link HttpServletResponse}
     *
     * @param httpMethodProxyRequest An object representing the proxy request to be made
     * @param httpServletResponse    An object by which we can send the proxied response back to the client
     * @param digest
     * @throws ServletException Can be thrown to indicate that another error has occurred
     */
    private void executeProxyRequest(HttpRequestBase httpMethodProxyRequest,
                                     HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                     String user, String password) throws ServletException {

        if (user != null && password != null) {
            Credentials credentials = new UsernamePasswordCredentials(user, password);
            credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY, credentials);
            clientBuilder.setDefaultCredentialsProvider(credsProvider);
            httpClient = null;
            httpClient = createHttpClient();
        }

        InputStream inputStreamServerResponse = null;

        try {

            // //////////////////////////
            // Execute the request
            // //////////////////////////

            HttpResponse response = httpClient.execute(httpMethodProxyRequest);

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
                    throw new ServletException("Received status code: " + stringStatusCode
                            + " but no " + Utils.LOCATION_HEADER
                            + " header was found in the response");
                }

                // /////////////////////////////////////////////
                // Modify the redirect to go to this proxy
                // servlet rather that the proxied host
                // /////////////////////////////////////////////

                String redirectURL = httpServletRequest.getRequestURL() + "?url=" + URLEncoder.encode(stringLocation, "UTF-8");
                httpServletResponse.sendRedirect(redirectURL);
                LOGGER.info("redirected to:" + redirectURL);
                return;

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

                return;
            }

            // /////////////////////////////////////////////
            // Pass the response code back to the client
            // /////////////////////////////////////////////

            httpServletResponse.setStatus(getStatusCode(response));

            // /////////////////////////////////////////////
            // Pass response headers back to the client
            // /////////////////////////////////////////////

            Header[] headerArrayResponse = httpMethodProxyRequest.getAllHeaders();

            for (Header header : headerArrayResponse) {

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
//                else if (header.getName().equalsIgnoreCase(Utils.HTTP_HEADER_WWW_AUTHENTICATE))
//                    continue;                
                else
                    httpServletResponse.setHeader(header.getName(), header.getValue());
            }

            // ///////////////////////////////////
            // Send the content to the client
            // ///////////////////////////////////

            inputStreamServerResponse = response.getEntity().getContent();

            if (inputStreamServerResponse != null) {
                byte[] b = new byte[proxyConfig.getDefaultStreamByteSize()];

                int read = 0;
                ServletOutputStream out = httpServletResponse.getOutputStream();
                while ((read = inputStreamServerResponse.read(b)) > 0) {
                    out.write(b, 0, read);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error executing HTTP method", e);
        } finally {
            try {
                if (inputStreamServerResponse != null)
                    inputStreamServerResponse.close();
            } catch (IOException e) {
                LOGGER.error("Error closing request input stream", e);
                throw new ServletException(e.getMessage());
            }

            httpMethodProxyRequest.releaseConnection();
        }
    }

    int getStatusCode(HttpResponse response) {
        if (response != null) {
            StatusLine statusLine = response.getStatusLine();
            return statusLine.getStatusCode();
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
    @SuppressWarnings("rawtypes")
    private ProxyInfo setProxyRequestHeaders(URL url, HttpServletRequest httpServletRequest,
                                             HttpRequestBase httpMethodProxyRequest) {

        final String proxyHost = url.getHost();
        final int proxyPort = url.getPort();
        final String proxyPath = url.getPath();
        final ProxyInfo proxyInfo = new ProxyInfo(proxyHost, proxyPath, proxyPort);

        // ////////////////////////////////////////
        // Get an Enumeration of all the header
        // names sent by the client.
        // ////////////////////////////////////////

        Enumeration enumerationOfHeaderNames = httpServletRequest.getHeaderNames();

        while (enumerationOfHeaderNames.hasMoreElements()) {
            String stringHeaderName = (String) enumerationOfHeaderNames.nextElement();

            if (stringHeaderName.equalsIgnoreCase(Utils.CONTENT_LENGTH_HEADER_NAME))
                continue;

            // ////////////////////////////////////////////////////////////////////////
            // As per the Java Servlet API 2.5 documentation:
            // Some headers, such as Accept-Language can be sent by clients
            // as several headers each with a different value rather than
            // sending the header as a comma separated list.
            // Thus, we get an Enumeration of the header values sent by the client
            // ////////////////////////////////////////////////////////////////////////

            Enumeration enumerationOfHeaderValues = httpServletRequest.getHeaders(stringHeaderName);

            while (enumerationOfHeaderValues.hasMoreElements()) {
                String stringHeaderValue = (String) enumerationOfHeaderValues.nextElement();

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

    private Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    /**
     * @return int the maximum file upload size.
     */
    public int getMaxFileUploadSize() {
        return maxFileUploadSize;
    }

    /**
     * @return the client
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * set the httpClient
     *
     * @param httpClient the client to set
     */
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
