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
import java.util.Set;

/**
 * ProxyConfig interface to load the proxy configuration.
 * 
 * @author Alejandro Diaz
 */
public interface ProxyConfig {

    /**
     * Provide the proxy configuration
     * 
     * @throws IOException
     */
	public void configProxy();

    /**
     * @return the soTimeout
     */
    public int getSoTimeout();

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout();

    /**
     * @return the maxTotalConnections
     */
    public int getMaxTotalConnections();

    /**
     * @return the defaultMaxConnectionsPerHost
     */
    public int getDefaultMaxConnectionsPerHost();

    /**
     * @return the hostnameWhitelist
     */
    public Set<String> getHostnameWhitelist();

    /**
     * @return the mimetypeWhitelist
     */
    public Set<String> getMimetypeWhitelist();

    /**
     * @return the reqtypeWhitelist
     */
    public Set<String> getReqtypeWhitelist();

    /**
     * @return the methodsWhitelist
     */
    public Set<String> getMethodsWhitelist();
    
    /**
     * @return the hostsWhitelist
     */
    public Set<String> getHostsWhitelist();
    
    /**
	 * @return the defaultStreamByteSize
	 */
	public int getDefaultStreamByteSize();

}
