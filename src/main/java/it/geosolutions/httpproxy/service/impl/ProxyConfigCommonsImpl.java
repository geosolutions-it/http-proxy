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
package it.geosolutions.httpproxy.service.impl;


import it.geosolutions.httpproxy.service.ProxyConfig;
import it.geosolutions.httpproxy.utils.Utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * ProxyConfig implementation reading proxyProperties as commons configuration.
 * 
 * @author Alejandro Diaz
 */
public final class ProxyConfigCommonsImpl implements ProxyConfig {

    private final static Logger LOGGER = Logger.getLogger(ProxyConfigCommonsImpl.class.toString());

    /**
     * A list of regular expressions describing hostnames the proxy is permitted to forward to
     */
    private Set<String> hostnameWhitelist = new HashSet<String>();

    /**
     * A list of regular expressions describing MIMETypes the proxy is permitted to forward
     */
    private Set<String> mimetypeWhitelist = new HashSet<String>();

    /**
     * A list of regular expressions describing Request Types the proxy is permitted to forward
     */
    private Set<String> reqtypeWhitelist = new HashSet<String>();

    /**
     * A list of regular expressions describing request METHODS the proxy is permitted to forward
     */
    private Set<String> methodsWhitelist = new HashSet<String>();

    /**
     * A list of regular expressions describing request HOSTS the proxy is permitted to forward
     */
    private Set<String> hostsWhitelist = new HashSet<String>();

    /**
     * The path of the properties file
     */
    private String propertiesFilePath;

    /**
     * The request timeout
     */
    private int soTimeout = 30000;

    /**
     * The maximum total connections available
     */
    private int maxTotalConnections = 60;

    /**
     * The maximum connections available per host
     */
    private int defaultMaxConnectionsPerHost = 6;
    
    private int defaultStreamByteSize = 1024;
    
    /**
     * Proxy properties autowired
     */
    @Autowired @Qualifier("proxyProperties")
    private Configuration proxyProperties;

	/**
	 * Default constructor
     */
    public ProxyConfigCommonsImpl() {
       super();
    }

	/**
	 * Create a new proxy configuration with proxy properties
	 * 
     * @param proxyProperties
     */
    public ProxyConfigCommonsImpl(Configuration proxyProperties) {
        this.proxyProperties = proxyProperties;
        configProxy();
    }

    /**
     * Provide the proxy configuration
     * 
     * @throws IOException
     */
    public void configProxy() {
    	Configuration props = propertiesLoader();
        if (props != null) {
            try {
                // ////////////////////////////////////////////////////////////
                // Load properties in getters from properties file
                // ////////////////////////////////////////////////////////////
                getHostnameWhitelist();
                getMimetypeWhitelist();
                getMethodsWhitelist();
                getHostnameWhitelist();
                getReqtypeWhitelist();
                getDefaultStreamByteSize();
                getConnectionTimeout();
                getMaxTotalConnections();
            } catch (NumberFormatException e) {
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE,
                            "Error parsing the proxy properties file using default", e);
            }
        }
    }

    /**
     * Read the proxy properties file.
     * 
     * @return Properties
     */
    public Configuration propertiesLoader() {
    	if(this.proxyProperties != null){
    		return this.proxyProperties;
    	}else{
            try {
        		Configuration config = new PropertiesConfiguration(propertiesFilePath);
        		((PropertiesConfiguration) config).setReloadingStrategy(new FileChangedReloadingStrategy());
        		((PropertiesConfiguration) config).load();
                return config;

            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE, "Error loading the proxy properties file ", e);
                return null;
            }
    	}
    }

    /**
     * @return the soTimeout
     */
    public int getSoTimeout() {
        return soTimeout;
    }

    /**
     * @param soTimeout the soTimeout to set
     */
    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout() {
    	String timeout = propertiesLoader().getString("timeout");
    	return timeout != null ? Integer.parseInt(timeout) : this.soTimeout;
    }

    /**
     * @return the maxTotalConnections
     */
    public int getMaxTotalConnections() {
        String max_conn = propertiesLoader().getString("max_total_connections");
        return max_conn != null ? Integer.parseInt(max_conn)
                : this.maxTotalConnections;
    }

    /**
     * @return the defaultMaxConnectionsPerHost
     */
    public int getDefaultMaxConnectionsPerHost() {
    	String def_conn_host = propertiesLoader().getString("default_max_connections_per_host");
        return def_conn_host != null ? Integer.parseInt(def_conn_host)
                : this.defaultMaxConnectionsPerHost;
    }

    /**
     * @return the hostnameWhitelist
     */
    public Set<String> getHostnameWhitelist() {
        Configuration props = propertiesLoader();

        if (props != null) {
            Set<String> set = Utils.getProperty(props, "hostnameWhitelist");
            if (set != null)
                this.setHostnameWhitelist(set);
        }

        return hostnameWhitelist;
    }

    /**
     * @param hostnameWhitelist the hostnameWhitelist to set
     */
    public void setHostnameWhitelist(Set<String> hostnameWhitelist) {
        this.hostnameWhitelist = hostnameWhitelist;
    }

    /**
     * @return the mimetypeWhitelist
     */
    public Set<String> getMimetypeWhitelist() {
        Configuration props = propertiesLoader();

        if (props != null) {
            Set<String> set = Utils.getProperty(props, "mimetypeWhitelist");
            if (set != null)
                this.setMimetypeWhitelist(set);
        }

        return mimetypeWhitelist;
    }

    /**
     * @param mimetypeWhitelist the mimetypeWhitelist to set
     */
    public void setMimetypeWhitelist(Set<String> mimetypeWhitelist) {
        this.mimetypeWhitelist = mimetypeWhitelist;
    }

    /**
     * @return the reqtypeWhitelist
     */
    public Set<String> getReqtypeWhitelist() {
        Configuration props = propertiesLoader();

        if (props != null) {
            Set<String> rt = new HashSet<String>();
            String s = props.getString("reqtypeWhitelist.capabilities");
            if (s != null)
                rt.add(s);

            s = props.getString("reqtypeWhitelist.geostore");
            if (s != null)
                rt.add(s);

            s = props.getString("reqtypeWhitelist.csw");
            if (s != null)
                rt.add(s);
            
            s = props.getString("reqtypeWhitelist.featureinfo");
            if (s != null)
                rt.add(s);
            
            s = props.getString("reqtypeWhitelist.generic");
            if (s != null)
                rt.add(s);

            this.setReqtypeWhitelist(rt);
        }

        return reqtypeWhitelist;
    }

    /**
     * @param reqtypeWhitelist the reqtypeWhitelist to set
     */
    public void setReqtypeWhitelist(Set<String> reqtypeWhitelist) {
        this.reqtypeWhitelist = reqtypeWhitelist;
    }

    /**
     * @return the methodsWhitelist
     */
    public Set<String> getMethodsWhitelist() {
        Configuration props = propertiesLoader();

        if (props != null) {
            Set<String> set = Utils.getProperty(props, "methodsWhitelist");
            if (set != null)
                this.setMethodsWhitelist(set);
        }

        return methodsWhitelist;
    }

    /**
     * @param methodsWhitelist the methodsWhitelist to set
     */
    public void setMethodsWhitelist(Set<String> methodsWhitelist) {
        this.methodsWhitelist = methodsWhitelist;
    }

    /**
     * @return the hostsWhitelist
     */
    public Set<String> getHostsWhitelist() {
        Configuration props = propertiesLoader();

        if (props != null) {
            Set<String> set = Utils.getProperty(props, "hostsWhitelist");
            if (set != null)
                this.setHostsWhitelist(set);
        }

        return hostsWhitelist;
    }

    /**
     * @param hostsWhitelist the hostsWhitelist to set
     */
    public void setHostsWhitelist(Set<String> hostsWhitelist) {
        this.hostsWhitelist = hostsWhitelist;
    }

    /**
     * @return the propertiesFilePath
     */
    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    /**
     * @param propertiesFilePath the propertiesFilePath to set
     */
    public void setPropertiesFilePath(String propertiesFilePath) {
        this.propertiesFilePath = propertiesFilePath;
    }
    
    /**
	 * @return the defaultStreamByteSize
	 */
	public int getDefaultStreamByteSize() {
        Configuration props = propertiesLoader();
        String bytesSize = props.getString("defaultStreamByteSize");
        return bytesSize != null ? Integer.parseInt(bytesSize) : 
        	this.defaultStreamByteSize;
	}

}
