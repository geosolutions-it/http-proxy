package it.geosolutions.httpproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

/**
 * HTTPProxy class.
 * 
 * @author Alessio Fabiani at alessio.fabiani@geo-solutions.it
 * @author Tobia di Pisa at tobia.dipisa@geo-solutions.it
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class HTTPProxy extends HttpServlet {
    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * The maximum size for uploaded files in bytes. Default value is 5MB.
     */
    private int maxFileUploadSize = Utils.DEFAULT_MAX_FILE_UPLOAD_SIZE;

    /**
     * Performs an HTTP GET request
     * 
     * @param httpServletRequest
     *            The {@link HttpServletRequest} object passed in by the servlet engine representing
     *            the client request to be proxied
     * @param httpServletResponse
     *            The {@link HttpServletResponse} object by which we can send a proxied response to
     *            the client
     */
    @SuppressWarnings("rawtypes")
    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

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
            // Create a GET request
            GetMethod getMethodProxyRequest = new GetMethod(url.toExternalForm());

            // Forward the request headers
            final ProxyInfo proxyInfo=setProxyRequestHeaders(url, httpServletRequest, getMethodProxyRequest);

            // Execute the proxy request
            this.executeProxyRequest(getMethodProxyRequest, httpServletRequest,
                    httpServletResponse, user, password,proxyInfo);
        }
    }

    /**
     * Performs an HTTP POST request
     * 
     * @param httpServletRequest
     *            The {@link HttpServletRequest} object passed in by the servlet engine representing
     *            the client request to be proxied
     * @param httpServletResponse
     *            The {@link HttpServletResponse} object by which we can send a proxied response to
     *            the client
     */
    @SuppressWarnings("rawtypes")
    public void doPost(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws IOException, ServletException {

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
            // Create a standard POST request
            PostMethod postMethodProxyRequest = new PostMethod(url.toExternalForm());

            // Forward the request headers
            final ProxyInfo proxyInfo=setProxyRequestHeaders(url, httpServletRequest, postMethodProxyRequest);

            // Check if this is a mulitpart (file upload) POST
            if (ServletFileUpload.isMultipartContent(httpServletRequest)) {
                this.handleMultipartPost(postMethodProxyRequest, httpServletRequest);
            } else {
                this.handleStandardPost(postMethodProxyRequest, httpServletRequest);
            }

            // Execute the proxy request
            this.executeProxyRequest(postMethodProxyRequest, httpServletRequest,
                    httpServletResponse, user, password,proxyInfo);
        }
    }

    /**
     * Performs an HTTP PUT request
     * 
     * @param httpServletRequest
     *            The {@link HttpServletRequest} object passed in by the servlet engine representing
     *            the client request to be proxied
     * @param httpServletResponse
     *            The {@link HttpServletResponse} object by which we can send a proxied response to
     *            the client
     */
    @SuppressWarnings("rawtypes")
    public void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

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
            // Create a standard PUT request
            PutMethod putMethodProxyRequest = new PutMethod(url.toExternalForm());

            // Forward the request headers
            final ProxyInfo proxyInfo=setProxyRequestHeaders(url, httpServletRequest, putMethodProxyRequest);

            // Execute the proxy request
            this.executeProxyRequest(putMethodProxyRequest, httpServletRequest,
                    httpServletResponse, user, password,proxyInfo);
        }
    }

    /**
     * Sets up the given {@link PostMethod} to send the same multipart POST data as was sent in the
     * given {@link HttpServletRequest}
     * 
     * @param postMethodProxyRequest
     *            The {@link PostMethod} that we are configuring to send a multipart POST request
     * @param httpServletRequest
     *            The {@link HttpServletRequest} that contains the mutlipart POST data to be sent
     *            via the {@link PostMethod}
     */
    @SuppressWarnings("unchecked")
    private void handleMultipartPost(PostMethod postMethodProxyRequest,
            HttpServletRequest httpServletRequest) throws ServletException {
        // Create a factory for disk-based file items
        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();

        // Set factory constraints
        diskFileItemFactory.setSizeThreshold(this.getMaxFileUploadSize());
        diskFileItemFactory.setRepository(Utils.DEFAULT_FILE_UPLOAD_TEMP_DIRECTORY);

        // Create a new file upload handler
        ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);

        // Parse the request
        try {
            // Get the multipart items as a list
            List<FileItem> listFileItems = (List<FileItem>) servletFileUpload
                    .parseRequest(httpServletRequest);

            // Create a list to hold all of the parts
            List<Part> listParts = new ArrayList<Part>();

            // Iterate the multipart items list
            for (FileItem fileItemCurrent : listFileItems) {
                // If the current item is a form field, then create a string part
                if (fileItemCurrent.isFormField()) {
                    StringPart stringPart = new StringPart(fileItemCurrent.getFieldName(), // The
                                                                                           // field
                                                                                           // name
                            fileItemCurrent.getString() // The field value
                    );

                    // Add the part to the list
                    listParts.add(stringPart);
                } else {
                    // The item is a file upload, so we create a FilePart
                    FilePart filePart = new FilePart(fileItemCurrent.getFieldName(), // The field
                                                                                     // name
                            new ByteArrayPartSource(fileItemCurrent.getName(), // The uploaded file
                                                                               // name
                                    fileItemCurrent.get() // The uploaded file contents
                            ));

                    // Add the part to the list
                    listParts.add(filePart);
                }
            }

            MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(
                    listParts.toArray(new Part[] {}), postMethodProxyRequest.getParams());

            postMethodProxyRequest.setRequestEntity(multipartRequestEntity);

            // The current content-type header (received from the client) IS of
            // type "multipart/form-data", but the content-type header also
            // contains the chunk boundary string of the chunks. Currently, this
            // header is using the boundary of the client request, since we
            // blindly copied all headers from the client request to the proxy
            // request. However, we are creating a new request with a new chunk
            // boundary string, so it is necessary that we re-set the
            // content-type string to reflect the new chunk boundary string
            postMethodProxyRequest.setRequestHeader(Utils.CONTENT_TYPE_HEADER_NAME,
                    multipartRequestEntity.getContentType());

        } catch (FileUploadException fileUploadException) {
            throw new ServletException(fileUploadException);
        }
    }

    /**
     * Sets up the given {@link PostMethod} to send the same standard POST data as was sent in the
     * given {@link HttpServletRequest}
     * 
     * @param postMethodProxyRequest
     *            The {@link PostMethod} that we are configuring to send a standard POST request
     * @param httpServletRequest
     *            The {@link HttpServletRequest} that contains the POST data to be sent via the
     *            {@link PostMethod}
     */
    @SuppressWarnings("deprecation")
    private void handleStandardPost(PostMethod postMethodProxyRequest,
            HttpServletRequest httpServletRequest) {
        try {
            postMethodProxyRequest.setRequestBody(httpServletRequest.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes the {@link HttpMethod} passed in and sends the proxy response back to the client via
     * the given {@link HttpServletResponse}
     * 
     * @param httpMethodProxyRequest
     *            An object representing the proxy request to be made
     * @param httpServletResponse
     *            An object by which we can send the proxied response back to the client
     * @param digest
     * @throws IOException
     *             Can be thrown by the {@link HttpClient}.executeMethod
     * @throws ServletException
     *             Can be thrown to indicate that another error has occurred
     */
    private void executeProxyRequest(
            HttpMethod httpMethodProxyRequest,
            HttpServletRequest httpServletRequest, 
            HttpServletResponse httpServletResponse,
            String user, 
            String password,
            ProxyInfo proxyInfo) throws IOException, ServletException {

        // Create a default HttpClient
        HttpClient httpClient = new HttpClient();

        if (user != null && password != null) {
            UsernamePasswordCredentials upc = new UsernamePasswordCredentials(user, password);
            httpClient.getState().setCredentials(AuthScope.ANY, upc);
        }

        httpMethodProxyRequest.setFollowRedirects(false);

        // Execute the request
        int intProxyResponseCode = httpClient.executeMethod(httpMethodProxyRequest);

        // Check if the proxy response is a redirect
        // The following code is adapted from org.tigris.noodle.filters.CheckForRedirect
        // Hooray for open source software
        if (intProxyResponseCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
                && intProxyResponseCode < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {

            String stringStatusCode = Integer.toString(intProxyResponseCode);
            String stringLocation = httpMethodProxyRequest
                    .getResponseHeader(Utils.LOCATION_HEADER).getValue();

            if (stringLocation == null) {
                throw new ServletException("Recieved status code: " + stringStatusCode + " but no "
                        + Utils.LOCATION_HEADER + " header was found in the response");
            }

            // Modify the redirect to go to this proxy servlet rather that the proxied host
            String stringMyHostName = httpServletRequest.getServerName();

            if (httpServletRequest.getServerPort() != 80) {
                stringMyHostName += ":" + httpServletRequest.getServerPort();
            }

            stringMyHostName += httpServletRequest.getContextPath();
            httpServletResponse.sendRedirect(stringLocation.replace(
                    Utils.getProxyHostAndPort(proxyInfo) + proxyInfo.getProxyPath(), stringMyHostName));

            return;
        } else if (intProxyResponseCode == HttpServletResponse.SC_NOT_MODIFIED) {
            // 304 needs special handling. See:
            // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
            // We get a 304 whenever passed an 'If-Modified-Since'
            // header and the data on disk has not changed; server
            // responds w/ a 304 saying I'm not going to send the
            // body because the file has not changed.
            httpServletResponse.setIntHeader(Utils.CONTENT_LENGTH_HEADER_NAME, 0);
            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);

            return;
        }

        // Pass the response code back to the client
        httpServletResponse.setStatus(intProxyResponseCode);

        // Pass response headers back to the client
        Header[] headerArrayResponse = httpMethodProxyRequest.getResponseHeaders();

        for (Header header : headerArrayResponse) {
            // Skip GZIP Responses
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

        // Send the content to the client
        InputStream inputStreamServerResponse = httpMethodProxyRequest.getResponseBodyAsStream();
        OutputStream outputStreamClientResponse = httpServletResponse.getOutputStream();

        int read=0;
        while ((read = inputStreamServerResponse.read()) > 0) {
            if (Utils.escapeHtmlFull(read) > 0) {
                outputStreamClientResponse.write(read);
            }
        }

        inputStreamServerResponse.close();

        outputStreamClientResponse.write('\n');
        outputStreamClientResponse.flush();
        outputStreamClientResponse.close();
    }

    /**
     * Retreives all of the headers from the servlet request and sets them on the proxy request
     * 
     * @param httpServletRequest
     *            The request object representing the client's request to the servlet engine
     * @param httpMethodProxyRequest
     *            The request that we are about to send to the proxy host
     * @return 
     */
    @SuppressWarnings("rawtypes")
    private ProxyInfo setProxyRequestHeaders(URL url, HttpServletRequest httpServletRequest,
            HttpMethod httpMethodProxyRequest) {
        final String proxyHost = url.getHost();
        final int proxyPort = url.getPort();
        final String proxyPath = url.getPath();
        final ProxyInfo proxyInfo=new ProxyInfo(proxyHost, proxyPath, proxyPort);

        // Get an Enumeration of all of the header names sent by the client
        Enumeration enumerationOfHeaderNames = httpServletRequest.getHeaderNames();

        while (enumerationOfHeaderNames.hasMoreElements()) {
            String stringHeaderName = (String) enumerationOfHeaderNames.nextElement();

            if (stringHeaderName.equalsIgnoreCase(Utils.CONTENT_LENGTH_HEADER_NAME))
                continue;

            // As per the Java Servlet API 2.5 documentation:
            // Some headers, such as Accept-Language can be sent by clients
            // as several headers each with a different value rather than
            // sending the header as a comma separated list.
            // Thus, we get an Enumeration of the header values sent by the client
            Enumeration enumerationOfHeaderValues = httpServletRequest.getHeaders(stringHeaderName);

            while (enumerationOfHeaderValues.hasMoreElements()) {
                String stringHeaderValue = (String) enumerationOfHeaderValues.nextElement();

                // In case the proxy host is running multiple virtual servers,
                // rewrite the Host header to ensure that we get content from
                // the correct virtual server

                if (stringHeaderName.equalsIgnoreCase(Utils.HOST_HEADER_NAME)) {
                    stringHeaderValue = Utils.getProxyHostAndPort(proxyInfo);
                }

                // Skip GZIP Responses
                if (stringHeaderName.equalsIgnoreCase(Utils.HTTP_HEADER_ACCEPT_ENCODING)
                        && stringHeaderValue.toLowerCase().contains("gzip"))
                    continue;
                if (stringHeaderName.equalsIgnoreCase(Utils.HTTP_HEADER_CONTENT_ENCODING)
                        && stringHeaderValue.toLowerCase().contains("gzip"))
                    continue;
                if (stringHeaderName.equalsIgnoreCase(Utils.HTTP_HEADER_TRANSFER_ENCODING))
                    continue;

                Header header = new Header(stringHeaderName, stringHeaderValue);

                // Set the same header on the proxy request
                httpMethodProxyRequest.setRequestHeader(header);
            }
            
        }
        return proxyInfo;
    }

    public int getMaxFileUploadSize() {
        return maxFileUploadSize;
    }

}
