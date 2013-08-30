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
package it.geosolutions.httpproxy.utils;


import java.net.URL;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;

/**
 * Proxy method configuration.
 * This class encapsulates the common information to execute a proxy method 
 * and is responsible of onInit call and the method generation. 
 * 
 * @author <a href="mailto:aledt84@gmail.com">Alejandro Diaz Torres</a>
 *
 */
public class ProxyMethodConfig {
	
	private URL url;
	private String user;
	private String password = null;
	private String method;
    
    /**
     * Proxy method contructor
     * 
     * @param url
     * @param user
     * @param password
     * @param method
     */
	public ProxyMethodConfig(URL url, String user, String password, String method) {
		super();
		this.url = url;
		this.user = user;
		this.password = password;
		this.method = method;
	}
	
	/**
	 * Obtain he method generation with {@link ProxyMethodConfig#method} and {@link ProxyMethodConfig#url}
	 * 
	 * @return HttpMethod only supports {DELETE, GET, POST and PUT} @see {@link HttpMethods}
	 */
	public HttpMethod getMethod(){
		if(HttpMethods.METHOD_DELETE.equals(method)){
			return new DeleteMethod(url.toExternalForm());
		}else if(HttpMethods.METHOD_GET.equals(method)){
			return new GetMethod(url.toExternalForm());
		}else if(HttpMethods.METHOD_POST.equals(method)){
			return new PostMethod(url.toExternalForm());
		}else if(HttpMethods.METHOD_PUT.equals(method)){
			return new PutMethod(url.toExternalForm());
		}else{
			// Default is doGet
			return new GetMethod(url.toExternalForm());
		}
	}
	
    public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setMethod(String method) {
		this.method = method;
	}
    
}
