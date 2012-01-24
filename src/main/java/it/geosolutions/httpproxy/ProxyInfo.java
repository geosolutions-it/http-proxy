/**
 * 
 */
package it.geosolutions.httpproxy;


/**
 * Simple placeholder class for the proxy information.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
final class ProxyInfo {

    private static final String DEFAULT_PROXY_APTH = "";
    // Proxy host params
    /**
     * The host to which we are proxying requests
     */
    private String proxyHost;
    /**
     * The (optional) path on the proxy host to wihch we are proxying requests. Default value is "".
     */
    private String proxyPath = DEFAULT_PROXY_APTH;
    /**
     * The port on the proxy host to wihch we are proxying requests. Default value is 80.
     */
    private int proxyPort = Utils.DEFAULT_PROXY_PORT;
    
    public ProxyInfo(String proxyHost, String proxyPath, int proxyPort) {
        super();
        this.proxyHost = proxyHost;
        this.proxyPath = proxyPath;
        this.proxyPort = proxyPort;
    }
    
    public String getProxyHost() {
        return proxyHost;
    }
    
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }
    
    public String getProxyPath() {
        return proxyPath;
    }
    
    public void setProxyPath(String proxyPath) {
        this.proxyPath = proxyPath;
    }
    
    public int getProxyPort() {
        return proxyPort;
    }
    
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
}
