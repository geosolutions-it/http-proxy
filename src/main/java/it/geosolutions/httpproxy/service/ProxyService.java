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
package it.geosolutions.httpproxy.service;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethod;

/**
 * Proxy common interface
 * 
 * @author <a href="mailto:aledt84@gmail.com">Alejandro Diaz Torres</a>
 *
 */
public interface ProxyService {

	/**
	 * Change proxy configuration
	 * 
	 * @param new proxy configuration
	 */
	public void setProxyConfig(ProxyConfig proxyConfig);

	/**
	 * Obtain proxy configuration
	 * 
	 * @return proxy configuration
	 */
	public ProxyConfig getProxyConfig();

    /**
     * Performs an HTTP request. Read <code>httpServletRequest</code> method. Default method is HTTP GET. 
     * 
     * @param httpServletRequest The {@link HttpServletRequest} object passed in by the servlet engine representing the client request to be proxied
     * @param httpServletResponse The {@link HttpServletResponse} object by which we can send a proxied response to the client
     */
    public void execute(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws  IOException, ServletException;

    /**
     * Method to be called on execute method initialization
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    public void onInit(HttpServletRequest request, HttpServletResponse response, URL url)
            throws IOException;

    /**
     * Method to be called to handle the remote response
     * 
     * @param method
     * @throws IOException
     */
    public void onRemoteResponse(HttpMethod method) throws IOException;

    /**
     * Method to be called when the method execute is finishing
     * 
     * @throws IOException
     */
    public void onFinish() throws IOException;

}
