/*
 *  Copyright (C) 2007 - 2013 GeoSolutions S.A.S.
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
package it.geosolutions.httpproxy.service.impl;

import it.geosolutions.httpproxy.callback.HostChecker;
import it.geosolutions.httpproxy.callback.HostNameChecker;
import it.geosolutions.httpproxy.callback.MethodsChecker;
import it.geosolutions.httpproxy.callback.MimeTypeChecker;
import it.geosolutions.httpproxy.callback.ProxyCallback;
import it.geosolutions.httpproxy.callback.RequestTypeChecker;
import it.geosolutions.httpproxy.exception.HttpErrorException;
import it.geosolutions.httpproxy.service.ProxyConfig;
import it.geosolutions.httpproxy.service.ProxyHelper;
import it.geosolutions.httpproxy.service.ProxyService;
import it.geosolutions.httpproxy.utils.ProxyInfo;
import it.geosolutions.httpproxy.utils.ProxyMethodConfig;
import it.geosolutions.httpproxy.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
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
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

/**
 * HTTPProxy delegated in this service
 * 
 * @author Alessio Fabiani at alessio.fabiani@geo-solutions.it
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Alejandro Diaz
 */
public class ProxyServiceImpl implements ProxyService, Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 3318254969779984284L;

	private final static Logger LOGGER = Logger.getLogger(ProxyServiceImpl.class.toString());

    /**
     * The maximum size for uploaded files in bytes. Default value is 5MB.
     */
    private int maxFileUploadSize = Utils.DEFAULT_MAX_FILE_UPLOAD_SIZE;

    /**
     * An Apache commons HTTP client backed by a multithreaded connection manager that allows to reuse connections to the backing server and to limit
     * the max number of concurrent connections.
     */
    private MultiThreadedHttpConnectionManager connectionManager;

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
    
    /**
     * Proxy helper to encapsulate proxy initialization
     */
    private ProxyHelper proxyHelper;

    /**
     * Default constructor
     */
	public ProxyServiceImpl(){
		super();
	}
    
	/**
	 * Constructor with helper
	 */
	public ProxyServiceImpl(ProxyHelper proxyHelper) {
		super();
		this.proxyHelper = proxyHelper;
        proxyHelper.initProxy(this);
	}

	/**
	 * Constructor with proxy configuration
	 */
	public ProxyServiceImpl(ProxyConfig proxyConfig) {
		super();
		this.setProxyConfig(proxyConfig);
	}
    
	/**
	 * Obtain proxy configuration
	 * 
	 * @return the proxy configuration
	 */
	public ProxyConfig getProxyConfig() {
		return proxyConfig;
	}

	/**
	 * Change proxy configuration
	 * @param new proxy configuration
	 */
	public void setProxyConfig(ProxyConfig proxyConfig) {
		this.proxyConfig = proxyConfig;
		loadProxyConfig();
	}
	
	/**
	 * Load proxy configuration when proxy config has changed
	 */
	private void loadProxyConfig(){
		connectionManager = new MultiThreadedHttpConnectionManager();
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();

		params.setSoTimeout(proxyConfig.getSoTimeout());
		params.setConnectionTimeout(proxyConfig.getConnectionTimeout());
		params.setMaxTotalConnections(proxyConfig.getMaxTotalConnections());
		params.setDefaultMaxConnectionsPerHost(proxyConfig
				.getDefaultMaxConnectionsPerHost());

		connectionManager.setParams(params);
		httpClient = new HttpClient(connectionManager);
		
		configureCallbacks();
	}

	/**
	 * Configure the proxy callbacks
	 */
	private void configureCallbacks() {
		
		if(callbacks != null){
			// Change callbacks configuration
			for(ProxyCallback callback: callbacks){
				callback.setProxyConfig(proxyConfig);
			}
		}else{
			// //////////////////////////////////////////
			// Setup the callbacks (should be loded 
			// in spring context).
			// //////////////////////////////////////////

			callbacks = new ArrayList<ProxyCallback>();
			callbacks.add(new MimeTypeChecker(proxyConfig));
			callbacks.add(new HostNameChecker(proxyConfig));
			callbacks.add(new RequestTypeChecker(proxyConfig));
			callbacks.add(new MethodsChecker(proxyConfig));
			callbacks.add(new HostChecker(proxyConfig));
		}
	}
	
	/**
     * Performs an HTTP request. Read <code>httpServletRequest</code> method. Default method is HTTP GET. 
     * 
     * @param httpServletRequest The {@link HttpServletRequest} object passed in by the servlet engine representing the client request to be proxied
     * @param httpServletResponse The {@link HttpServletResponse} object by which we can send a proxied response to the client
     */
	public void execute(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws  IOException, ServletException {
		try {
			this.doMethod(httpServletRequest, httpServletResponse);
		} catch (HttpErrorException ex) {
            httpServletResponse.sendError(ex.getCode(), ex.getMessage());
		} catch (Exception e) {
            httpServletResponse.sendError(HttpStatus.SC_SERVICE_UNAVAILABLE, e.getMessage());
		} finally {
            onFinish();
        }

	}

    /**
     * 
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    public void onInit(HttpServletRequest request, HttpServletResponse response, URL url)
            throws IOException {
        for (ProxyCallback callback : callbacks) {
            callback.onRequest(request, response, url);
        }
    }

    /**
     * @param method
     * @throws IOException
     */
    public void onRemoteResponse(HttpMethod method) throws IOException {
        for (ProxyCallback callback : callbacks) {
            callback.onRemoteResponse(method);
        }
    }

    /**
     * @throws IOException
     */
    public void onFinish() throws IOException {
        for (ProxyCallback callback : callbacks) {
            callback.onFinish();
        }
    }

    /**
     * Performs an HTTP request
     * 
     * @param httpServletRequest The {@link HttpServletRequest} object passed in by the servlet engine representing the client request to be proxied
     * @param httpServletResponse The {@link HttpServletResponse} object by which we can send a proxied response to the client
     */
    public void doMethod(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
		ProxyMethodConfig methodConfig = proxyHelper.prepareProxyMethod(
				httpServletRequest, httpServletResponse, this);

        if (methodConfig != null) {

            // //////////////////////////////
            // Create a request
            // //////////////////////////////

            HttpMethod methodProxyRequest = methodConfig.getMethod();

            // //////////////////////////////
            // Forward the request headers
            // //////////////////////////////

            final ProxyInfo proxyInfo = setProxyRequestHeaders(methodConfig.getUrl(), httpServletRequest,
            		methodProxyRequest);

            // //////////////////////////////////////////////////
            // Check if this is a mulitpart (file upload) PUT | POST
            // //////////////////////////////////////////////////

            if(methodProxyRequest instanceof EntityEnclosingMethod){
	            if (ServletFileUpload.isMultipartContent(httpServletRequest)) {
	                this.handleMultipart((EntityEnclosingMethod)methodProxyRequest, httpServletRequest);
	            } else {
	                this.handleStandard((EntityEnclosingMethod)methodProxyRequest, httpServletRequest);
	            }
            }

            // //////////////////////////////
            // Execute the proxy request
            // //////////////////////////////

            this.executeProxyRequest(methodProxyRequest, httpServletRequest,
                    httpServletResponse, methodConfig.getUser(), methodConfig.getPassword(), proxyInfo);

        }
    }

    /**
     * Sets up the given {@link PostMethod} to send the same multipart POST data as was sent in the given {@link HttpServletRequest}
     * 
     * @param postMethodProxyRequest The {@link PostMethod} that we are configuring to send a multipart POST request
     * @param httpServletRequest The {@link HttpServletRequest} that contains the mutlipart POST data to be sent via the {@link PostMethod}
     */
    private void handleMultipart(EntityEnclosingMethod methodProxyRequest,
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

            @SuppressWarnings("unchecked")
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
                            fileItemCurrent.getString());

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
                                    fileItemCurrent.get()));

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

            methodProxyRequest.setRequestHeader(Utils.CONTENT_TYPE_HEADER_NAME,
                    multipartRequestEntity.getContentType());

        } catch (FileUploadException fileUploadException) {
            throw new ServletException(fileUploadException);
        }
    }

    /**
     * Sets up the given {@link PostMethod} to send the same standard POST data as was sent in the given {@link HttpServletRequest}
     * 
     * @param postMethodProxyRequest The {@link PostMethod} that we are configuring to send a standard POST request
     * @param httpServletRequest The {@link HttpServletRequest} that contains the POST data to be sent via the {@link PostMethod}
     * @throws IOException
     */
    private void handleStandard(EntityEnclosingMethod methodProxyRequest,
            HttpServletRequest httpServletRequest) throws IOException {
		  try {
			 
		      methodProxyRequest.setRequestEntity(new InputStreamRequestEntity(httpServletRequest.getInputStream()));
		      //LOGGER.info("original request content length:" + httpServletRequest.getContentLength());
		      //LOGGER.info("proxied request content length:" +methodProxyRequest.getRequestEntity().getContentLength()+"");
		      
		      
		       
		  } catch (IOException e) {
		      throw new IOException(e);
		  }
    }

    /**
     * Executes the {@link HttpMethod} passed in and sends the proxy response back to the client via the given {@link HttpServletResponse}
     * 
     * @param httpMethodProxyRequest An object representing the proxy request to be made
     * @param httpServletResponse An object by which we can send the proxied response back to the client
     * @param digest
     * @throws IOException Can be thrown by the {@link HttpClient}.executeMethod
     * @throws ServletException Can be thrown to indicate that another error has occurred
     */
    private void executeProxyRequest(HttpMethod httpMethodProxyRequest,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            String user, String password, ProxyInfo proxyInfo) throws IOException, ServletException {

        if (user != null && password != null) {
            UsernamePasswordCredentials upc = new UsernamePasswordCredentials(user, password);
            httpClient.getState().setCredentials(AuthScope.ANY, upc);
        }

        httpMethodProxyRequest.setFollowRedirects(false);

        InputStream inputStreamServerResponse = null;
        ByteArrayOutputStream baos = null;
        
        try {

            // //////////////////////////
            // Execute the request
            // //////////////////////////

            int intProxyResponseCode = httpClient.executeMethod(httpMethodProxyRequest);

            onRemoteResponse(httpMethodProxyRequest);

            // ////////////////////////////////////////////////////////////////////////////////
            // Check if the proxy response is a redirect
            // The following code is adapted from
            // org.tigris.noodle.filters.CheckForRedirect
            // Hooray for open source software
            // ////////////////////////////////////////////////////////////////////////////////

            if (intProxyResponseCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
                    && intProxyResponseCode < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {

                String stringStatusCode = Integer.toString(intProxyResponseCode);
                String stringLocation = httpMethodProxyRequest.getResponseHeader(
                        Utils.LOCATION_HEADER).getValue();

                if (stringLocation == null) {
                    throw new ServletException("Recieved status code: " + stringStatusCode
                            + " but no " + Utils.LOCATION_HEADER
                            + " header was found in the response");
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
                        Utils.getProxyHostAndPort(proxyInfo) + proxyInfo.getProxyPath(),
                        stringMyHostName));

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
//                else if (header.getName().equalsIgnoreCase(Utils.HTTP_HEADER_WWW_AUTHENTICATE))
//                    continue;                
                else
                    httpServletResponse.setHeader(header.getName(), header.getValue());
            }

            // ///////////////////////////////////
            // Send the content to the client
            // ///////////////////////////////////
            
            inputStreamServerResponse = httpMethodProxyRequest
            		.getResponseBodyAsStream();
            
            if(inputStreamServerResponse != null){
                byte[] b = new byte[proxyConfig.getDefaultStreamByteSize()];
                
                baos = new ByteArrayOutputStream(b.length);
                
                int read = 0;
    		    while((read = inputStreamServerResponse.read(b)) > 0){ 
    		      	baos.write(b, 0, read);
    		        baos.flush();
    		    }
    	            
    		    baos.writeTo(httpServletResponse.getOutputStream());
            }
            
        } catch (HttpException e) {
            if (LOGGER.isLoggable(Level.SEVERE))
                LOGGER.log(Level.SEVERE, "Error executing HTTP method ", e);
        } finally {
			try {
	        	if(inputStreamServerResponse != null)
	        		inputStreamServerResponse.close();
			} catch (IOException e) {
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.log(Level.SEVERE,
							"Error closing request input stream ", e);
				throw new ServletException(e.getMessage());
			}
			
			try {
	        	if(baos != null){
	        		baos.flush();
	        		baos.close();
	        	}
			} catch (IOException e) {
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.log(Level.SEVERE,
							"Error closing response stream ", e);
				throw new ServletException(e.getMessage());
			}
        	
            httpMethodProxyRequest.releaseConnection();
        }
    }

    /**
     * Retrieves all of the headers from the servlet request and sets them on the proxy request
     * 
     * @param httpServletRequest The request object representing the client's request to the servlet engine
     * @param httpMethodProxyRequest The request that we are about to send to the proxy host
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
    
    /**
     * @return int the maximum file upload size.
     */
    public int getMaxFileUploadSize() {
        return maxFileUploadSize;
    }

	/**
	 * @return the callbacks
	 */
	public List<ProxyCallback> getCallbacks() {
		return callbacks;
	}

	/**
	 * @param callbacks the callbacks to set
	 */
	public void setCallbacks(List<ProxyCallback> callbacks) {
		this.callbacks = callbacks;
		configureCallbacks();
	}

	/**
	 * @return the proxyHelper
	 */
	public ProxyHelper getProxyHelper() {
		return proxyHelper;
	}

	/**
	 * @param proxyHelper the proxyHelper to set
	 */
	public void setProxyHelper(ProxyHelper proxyHelper) {
		this.proxyHelper = proxyHelper;
	}

}
