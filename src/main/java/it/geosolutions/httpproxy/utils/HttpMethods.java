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

import javax.servlet.http.HttpServlet;

/**
 * HttpMethods known methods for the proxy: [DELETE, GET, POST, PUT]
 * 
 * @author <a href="mailto:aledt84@gmail.com">Alejandro Diaz Torres</a>
 */
public class HttpMethods {

	/**
	 * {@link HttpServlet#METHOD_DELETE}
	 */
	public static final String METHOD_DELETE = "DELETE";

	/**
	 * {@link HttpServlet#METHOD_GET}
	 */
	public static final String METHOD_GET = "GET";

	/**
	 * {@link HttpServlet#METHOD_POST}
	 */
	public static final String METHOD_POST = "POST";

	/**
	 * {@link HttpServlet#METHOD_PUT}
	 */
	public static final String METHOD_PUT = "PUT";

}
