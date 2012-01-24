package it.geosolutions.httpproxy;

import java.util.LinkedHashSet;

/**
 * ProxyConfig class to define the proxy configuration.
 * 
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 */
final class ProxyConfig {

	/**
     *  A list of regular expressions describing hostnames the proxy is permitted to forward to 
     *  
     */
    private LinkedHashSet<String> hostnameWhitelist;

    /**
     *  A list of regular expressions describing MIMETypes the proxy is permitted to forward 
     *  
     */
    private LinkedHashSet<String> mimetypeWhitelist;
    
    /**
     *  A list of regular expressions describing Request Types the proxy is permitted to forward 
     *  
     */
    private LinkedHashSet<String> reqtypeWhitelist;
    
	/**
	 * Default constructor.
	 */
	public ProxyConfig() {
		super();
	}
	
	/**
	 * @param hostnameWhitelist
	 * @param mimetypeWhitelist
	 */
	public ProxyConfig(LinkedHashSet<String> hostnameWhitelist,
			LinkedHashSet<String> mimetypeWhitelist) {
		super();
		this.hostnameWhitelist = hostnameWhitelist;
		this.mimetypeWhitelist = mimetypeWhitelist;
	}
	
    /**
	 * @return the hostnameWhitelist
	 */
	public LinkedHashSet<String> getHostnameWhitelist() {
		return hostnameWhitelist;
	}

	/**
	 * @param hostnameWhitelist the hostnameWhitelist to set
	 */
	public void setHostnameWhitelist(LinkedHashSet<String> hostnameWhitelist) {
		this.hostnameWhitelist = hostnameWhitelist;
	}

	/**
	 * @return the mimetypeWhitelist
	 */
	public LinkedHashSet<String> getMimetypeWhitelist() {
		return mimetypeWhitelist;
	}

	/**
	 * @param mimetypeWhitelist the mimetypeWhitelist to set
	 */
	public void setMimetypeWhitelist(LinkedHashSet<String> mimetypeWhitelist) {
		this.mimetypeWhitelist = mimetypeWhitelist;
	}

	/**
	 * @return the reqtypeWhitelist
	 */
	public LinkedHashSet<String> getReqtypeWhitelist() {
		return reqtypeWhitelist;
	}

	/**
	 * @param reqtypeWhitelist the reqtypeWhitelist to set
	 */
	public void setReqtypeWhitelist(LinkedHashSet<String> reqtypeWhitelist) {
		this.reqtypeWhitelist = reqtypeWhitelist;
	}
	
}
