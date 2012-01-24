package it.geosolutions.httpproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
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
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

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

	private final static Logger LOGGER = Logger.getLogger(HTTPProxy.class.toString());
	
    /**
     * The maximum size for uploaded files in bytes. Default value is 5MB.
     */
    private int maxFileUploadSize = Utils.DEFAULT_MAX_FILE_UPLOAD_SIZE;
    
    /**
     * An Apache commons HTTP client backed by a multithreaded connection manager 
     * that allows to reuse connections to the backing server and to limit the max 
     * number of concurrent connections.
     */
    private MultiThreadedHttpConnectionManager connectionManager;
    
    /**
     * An HTTP "user-agent", containing an HTTP state and one or more HTTP 
     * connections, to which HTTP methods can be applied. 
     */
    private HttpClient httpClient;
    
    private ProxyConfig proxyConfig;

	/**
	 * Initialize the <code>ProxyServlet</code>
	 * @param servletConfig The Servlet configuration passed in by the servlet conatiner
	 */
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		InputStream inputStream = getServletContext().getResourceAsStream("/WEB-INF/classes/proxy.properties");
		Properties props = new Properties();
		
		try {
			props.load(inputStream);
		} catch (IOException e) {
			if(LOGGER.isLoggable(Level.SEVERE))
				LOGGER.log(Level.SEVERE, "Error loading the proxy properties file ", e);
		} 
		
		// ////////////////////////////////////////////////////////////
		// Load proxy configuration white lists from properties file
		// ////////////////////////////////////////////////////////////
		
		proxyConfig = new ProxyConfig();
		
		proxyConfig.setHostnameWhitelist(
				Utils.parseWhiteList(props.getProperty("hostnameWhitelist")));
		proxyConfig.setReqtypeWhitelist(
				Utils.parseWhiteList(props.getProperty("reqtypeWhitelist")));
		proxyConfig.setMimetypeWhitelist(
				Utils.parseWhiteList(props.getProperty("mimetypeWhitelist")));
		
		// /////////////////////////////////////////////////
		// Load connection manager configuration from
		// properties file.
		// /////////////////////////////////////////////////
		
        connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        
        try{
            params.setSoTimeout(
            		Integer.parseInt(props.getProperty("timeout")));
            params.setConnectionTimeout(
            		Integer.parseInt(props.getProperty("connection_timeout")));
            params.setMaxTotalConnections(
            		Integer.parseInt(props.getProperty("max_total_connections")));
            params.setDefaultMaxConnectionsPerHost(
            		Integer.parseInt(props.getProperty("default_max_connections_per_host")));
        }catch(NumberFormatException e){
			if(LOGGER.isLoggable(Level.SEVERE))
				LOGGER.log(Level.SEVERE, "Error parsing the proxy properties file ", e);
        }
        
        connectionManager.setParams(params);
        httpClient = new HttpClient(connectionManager);
	}
	
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
    public void doGet(
    		HttpServletRequest httpServletRequest, 
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
        	
        	// ///////////////////////////////////////////////////////
            // Check if this request is permitted to be forwarded
        	// ///////////////////////////////////////////////////////
        	
            if (checkPermission(url, httpServletRequest.getContentType()) != true) {
            	httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
            			"Request for not permitted content type, hostname or request type");
            }else{
            	
            	// //////////////////////////////
                // Create a GET request
            	// //////////////////////////////
            	
                GetMethod getMethodProxyRequest = new GetMethod(url.toExternalForm());

                // //////////////////////////////
                // Forward the request headers
                // //////////////////////////////
                
                final ProxyInfo proxyInfo = setProxyRequestHeaders(url, httpServletRequest, getMethodProxyRequest);

                // //////////////////////////////
                // Execute the proxy request
                // //////////////////////////////
                
                this.executeProxyRequest(
                		getMethodProxyRequest, 
                		httpServletRequest,
                        httpServletResponse, 
                        user, 
                        password, 
                        proxyInfo
                );
            }
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
    public void doPost(
    		HttpServletRequest httpServletRequest,
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
        	
        	// ///////////////////////////////////////////////////////
            // Check if this request is permitted to be forwarded
        	// ///////////////////////////////////////////////////////
        	
            if (checkPermission(url, httpServletRequest.getContentType()) != true) {
            	httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
            			"Request for not permitted content type, hostname or request type");
            }else{
            	
            	// /////////////////////////////////
                // Create a standard POST request
            	// /////////////////////////////////
            	
                PostMethod postMethodProxyRequest = new PostMethod(url.toExternalForm());

                // /////////////////////////////////
                // Forward the request headers
                // /////////////////////////////////
                
                final ProxyInfo proxyInfo = setProxyRequestHeaders(url, httpServletRequest, postMethodProxyRequest);

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
                
                this.executeProxyRequest(
                		postMethodProxyRequest, 
                		httpServletRequest,
                        httpServletResponse, 
                        user, 
                        password, 
                        proxyInfo
                );
            }
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
    public void doPut(
    		HttpServletRequest httpServletRequest, 
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
        	
        	// ///////////////////////////////////////////////////////
            // Check if this request is permitted to be forwarded
        	// ///////////////////////////////////////////////////////
        	
            if (checkPermission(url, httpServletRequest.getContentType()) != true) {
            	httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
            			"Request for not permitted content type, hostname or request type");
            }else{
            	
            	// ////////////////////////////////
                // Create a standard PUT request
            	// ////////////////////////////////
            	
                PutMethod putMethodProxyRequest = new PutMethod(url.toExternalForm());

                // ////////////////////////////////
                // Forward the request headers
                // ////////////////////////////////
                
                final ProxyInfo proxyInfo = setProxyRequestHeaders(url, httpServletRequest, putMethodProxyRequest);

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
                
                this.executeProxyRequest(
                		putMethodProxyRequest, 
                		httpServletRequest,
                        httpServletResponse, 
                        user, 
                        password,
                        proxyInfo
                );
            }
        }
    }
    
    /**
     * Performs an HTTP DELETE request
     * 
     * @param httpServletRequest
     *            The {@link HttpServletRequest} object passed in by the servlet engine representing
     *            the client request to be proxied
     * @param httpServletResponse
     *            The {@link HttpServletResponse} object by which we can send a proxied response to
     *            the client
     */
    public void doDelete(
    		HttpServletRequest httpServletRequest, 
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
        	
        	// ///////////////////////////////////////////////////////
            // Check if this request is permitted to be forwarded
        	// ///////////////////////////////////////////////////////
        	
            if (checkPermission(url, httpServletRequest.getContentType()) != true) {
            	httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
            			"Request for not permitted content type, hostname or request type");
            }else{
            	
            	// ////////////////////////////////
                // Create a standard DELETE request
            	// ////////////////////////////////
            	
                DeleteMethod deleteMethodProxyRequest = new DeleteMethod(url.toExternalForm());

                // ////////////////////////////////
                // Forward the request headers
                // ////////////////////////////////
                
                final ProxyInfo proxyInfo = setProxyRequestHeaders(url, httpServletRequest, deleteMethodProxyRequest);

                // ////////////////////////////////
                // Execute the proxy request
                // ////////////////////////////////
                
                this.executeProxyRequest(
                		deleteMethodProxyRequest, 
                		httpServletRequest,
                        httpServletResponse, 
                        user, 
                        password,
                        proxyInfo
                );
            }
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
    private void handleMultipart(
    		EntityEnclosingMethod methodProxyRequest,
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
    		// Create a list to hold all of the parts
    		// /////////////////////////////////////////
    		
    		List<Part> listParts = new ArrayList<Part>();

    		// /////////////////////////////////////////
    		// Iterate the multipart items list
    		// /////////////////////////////////////////
    		
    		for (FileItem fileItemCurrent : listFileItems) {
    			
    			// //////////////////////////////////////
    			// If the current item is a form field, 
    			// then create a string part
    			// //////////////////////////////////////
    			
    			if (fileItemCurrent.isFormField()) {
    				StringPart stringPart = new StringPart(
    						// The field name    						
    						fileItemCurrent.getFieldName(), 
    						// The field value						
    						fileItemCurrent.getString()
    				);

    				// ////////////////////////////
    				// Add the part to the list
    				// ////////////////////////////
    				
    				listParts.add(stringPart);
    				
    			} else {
    				
    				// /////////////////////////////////////////////////////
    				// The item is a file upload, so we create a FilePart
    				// /////////////////////////////////////////////////////
    				
    				FilePart filePart = new FilePart(
    						
    						// /////////////////////
    						// The field name
    						// /////////////////////
    						
    						fileItemCurrent.getFieldName(), 
    						
    						new ByteArrayPartSource(    								
    								// The uploaded file name
    								fileItemCurrent.getName(),    								
    								// The uploaded file contents
    								fileItemCurrent.get() 
    						));

    				// /////////////////////////////
    				// Add the part to the list
    				// /////////////////////////////
    				
    				listParts.add(filePart);
    			}
    		}

    		MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(
    				listParts.toArray(new Part[] {}), methodProxyRequest.getParams());

    		methodProxyRequest.setRequestEntity(multipartRequestEntity);

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
    		
    		methodProxyRequest.setRequestHeader(
    				Utils.CONTENT_TYPE_HEADER_NAME,
    				multipartRequestEntity.getContentType()
    	    );

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
     * @throws IOException 
     */
    private void handleStandard(
    		EntityEnclosingMethod methodProxyRequest,
            HttpServletRequest httpServletRequest) throws IOException {
        try {
        	methodProxyRequest.setRequestBody(httpServletRequest.getInputStream());
        } catch (IOException e) {
        	throw new IOException(e);
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

        if (user != null && password != null) {
            UsernamePasswordCredentials upc = new UsernamePasswordCredentials(user, password);
            httpClient.getState().setCredentials(AuthScope.ANY, upc);
        }

        httpMethodProxyRequest.setFollowRedirects(false);
        
        try {
        	
            // //////////////////////////
            // Execute the request
            // //////////////////////////
            
            int intProxyResponseCode = httpClient.executeMethod(httpMethodProxyRequest);
        
            // ////////////////////////////////////////////////////////////////////////////////
            // Check if the proxy response is a redirect
            // The following code is adapted from org.tigris.noodle.filters.CheckForRedirect
            // Hooray for open source software
            // ////////////////////////////////////////////////////////////////////////////////
            
            if (intProxyResponseCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
                    && intProxyResponseCode < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {

                String stringStatusCode = Integer.toString(intProxyResponseCode);
                String stringLocation = httpMethodProxyRequest
                        .getResponseHeader(Utils.LOCATION_HEADER).getValue();

                if (stringLocation == null) {
                    throw new ServletException("Recieved status code: " + stringStatusCode + " but no "
                            + Utils.LOCATION_HEADER + " header was found in the response");
                }
                
                // /////////////////////////////////////////////
                // Modify the redirect to go to this proxy 
                // servlet rather that the proxied host
                // /////////////////////////////////////////////
                
                String stringMyHostName = httpServletRequest.getServerName();

                if (httpServletRequest.getServerPort() != 80) {
                    stringMyHostName += ":" + httpServletRequest.getServerPort();
                }

                stringMyHostName += httpServletRequest.getContextPath();
                httpServletResponse.sendRedirect(stringLocation.replace(
                        Utils.getProxyHostAndPort(proxyInfo) + proxyInfo.getProxyPath(), stringMyHostName));

                return;
                
            } else if (intProxyResponseCode == HttpServletResponse.SC_NOT_MODIFIED) {
            	
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
            
            httpServletResponse.setStatus(intProxyResponseCode);

            // /////////////////////////////////////////////
            // Pass response headers back to the client
            // /////////////////////////////////////////////
            
            Header[] headerArrayResponse = httpMethodProxyRequest.getResponseHeaders();

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
                else
                    httpServletResponse.setHeader(header.getName(), header.getValue());
            }

            // ///////////////////////////////////
            // Send the content to the client
            // ///////////////////////////////////
            
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
            
        }catch(HttpException e) {
        	if(LOGGER.isLoggable(Level.SEVERE))
        		LOGGER.log(Level.SEVERE, "Error executing HTTP method ", e);
        }finally {
        	httpMethodProxyRequest.releaseConnection();
        }
    }

    /**
     * Retrieves all of the headers from the servlet request and sets them on the proxy request
     * 
     * @param httpServletRequest
     *            The request object representing the client's request to the servlet engine
     * @param httpMethodProxyRequest
     *            The request that we are about to send to the proxy host
     * @return ProxyInfo
     */
    @SuppressWarnings("rawtypes")
    private ProxyInfo setProxyRequestHeaders(URL url, HttpServletRequest httpServletRequest,
            HttpMethod httpMethodProxyRequest) {
    	
        final String proxyHost = url.getHost();
        final int proxyPort = url.getPort();
        final String proxyPath = url.getPath();
        final ProxyInfo proxyInfo = new ProxyInfo(proxyHost, proxyPath, proxyPort);

        // ////////////////////////////////////////
        // Get an Enumeration of all of the header 
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

                Header header = new Header(stringHeaderName, stringHeaderValue);

                // /////////////////////////////////////////////
                // Set the same header on the proxy request
                // /////////////////////////////////////////////
                
                httpMethodProxyRequest.setRequestHeader(header);
            }
        }
        
        return proxyInfo;
    }

    public int getMaxFileUploadSize() {
        return maxFileUploadSize;
    }
    
    /**
     * @param locator
     * @param contentType
     * @return boolean
     */
    private boolean checkPermission(URL locator, String contentType) {
    	
    	// ///////////////////////////////////////////////////////////
        // Check that the correct protocol is being used.
        // ///////////////////////////////////////////////////////////

    	if (locator.getProtocol().equals("http") || 
    			locator.getProtocol().equals("https")) {
    		
    		int hostsSize = proxyConfig.getHostnameWhitelist().size();
    		int mimeSize = proxyConfig.getMimetypeWhitelist().size();
    		int reqTypeSize = proxyConfig.getReqtypeWhitelist().size();
    		
            boolean hostnameOk = hostsSize == 0 ? true : false;
            boolean mimetypeOk = ((contentType == null || mimeSize == 0) ? true : false);
            boolean reqtypeOk = reqTypeSize == 0 ? true : false;
            
            // ///////////////////////////////////////////////////////
            // Check hostname and mimetype as appropriate to mode 
            // ///////////////////////////////////////////////////////
            
            if (hostsSize > 0) {
                hostnameOk = checkHostnamePermission(locator);
            }
            
            if (contentType != null && mimeSize > 0) {
                mimetypeOk = checkContentTypePermission(contentType);
            }
            
            if (reqTypeSize > 0) {
            	reqtypeOk = checkRequestTypePermission(locator);
            }
            
            // ///////////////////////////////////////////////////////
            // Return whether an action is permitted based on how 
            // proxy is configured.
            // ///////////////////////////////////////////////////////
            
            return hostnameOk && mimetypeOk && reqtypeOk;
        }
    	
    	// ///////////////////////////////////////////////////
        // The request is not permitted to go through.
        // ///////////////////////////////////////////////////
         
        return false;
	}

    /**
     * Checks a URL for whether its hostname is permitted.
     * 
     * @param locator A URL to check the permission status of
     * @return true if the hostname is permitted; otherwise false 
     */ 
    private boolean checkHostnamePermission(URL locator) {
    	
        // ////////////////////////////////
    	// Check  the whitelist of hosts 
    	// //////////////////////////////// 
    	
        if (proxyConfig.getHostnameWhitelist().contains(locator.getHost())) {
            return true;
        }
        
        // ////////////////////
        //otherwise say no
        // ////////////////////
        
        return false;
    }
    
    /**
     * Checks whether the content-type of a request is permitted by the proxy.
     * 
     * @param contentType A content type
     * @return true if the content-type is permitted; otherwise false 
     */
    private boolean checkContentTypePermission(String contentType) {
    	
    	// ///////////////////////////////////
        // Trim off extraneous information
    	// ///////////////////////////////////
    	
        String firstType = contentType.split(";")[0];
        
        // //////////////////////////////////////
        // Check off the content type 
        // provided vs. permitted content types
        // //////////////////////////////////////
        
        if (proxyConfig.getMimetypeWhitelist().contains(firstType)) {
            return true;
        }
        
        // /////////////////////
        // otherwise say no
        // /////////////////////
        
        return false;
    }

    /**
     * Checks a URL for whether its request type is permitted.
     * 
     * @param requestType A request type
     * @return true if the request type is permitted; otherwise false 
     */
    private boolean checkRequestTypePermission(URL locator) {
    	
        // //////////////////////////////////////
        // Check off the request type 
        // provided vs. permitted request types
        // //////////////////////////////////////
        
    	LinkedHashSet<String> list = proxyConfig.getReqtypeWhitelist();
    	Iterator<String> iterator = list.iterator();
    	
    	while(iterator.hasNext()){
    		String reqType = iterator.next();
    		
            if (locator.toExternalForm().indexOf(reqType) != -1) {
                return true;
            }
    	}
        
        // /////////////////////
        // otherwise say no
        // /////////////////////
        
        return false;
	}
    
}
