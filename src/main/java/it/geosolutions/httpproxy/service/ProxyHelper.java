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

import it.geosolutions.httpproxy.utils.ProxyMethodConfig;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Proxy helper interface.
 * This interface can initialize the proxy and prepare proxy method execution
 * 
 * @author <a href="mailto:aledt84@gmail.com">Alejandro Diaz Torres</a>
 *
 */
public interface ProxyHelper {
	
	/**
	 * Initialize proxy
	 * 
	 * @param proxy to be initialized
	 */
	public void initProxy(ProxyService proxy);
	
	/**
	 * Initialize proxy
	 * 
	 * @param proxy to be initialized
	 * @param context of the proxy
	 */
	public void initProxy(ProxyService proxy, ServletContext context);
	
	/**
	 * Prepare a proxy method execution
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @param proxy
	 * 
	 * @return ProxyMethodConfig to execute the method
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
	public ProxyMethodConfig prepareProxyMethod(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, ProxyService proxy)
			throws IOException, ServletException;

}
