package it.geosolutions.httpproxy;

import java.io.File;

/**
 * Utility methods.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
final class Utils {

    static final int DEFAULT_MAX_FILE_UPLOAD_SIZE = 5 * 1024 * 1024;
    /**
     * The directory to use to temporarily store uploaded files
     */
    static final File DEFAULT_FILE_UPLOAD_TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));
    /**
     * Key for redirect location header.
     */
    static final String LOCATION_HEADER = "Location";
    /**
     * Key for content type header.
     */
    static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    /**
     * Key for content length header.
     */
    static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    /**
     * Key for host header
     */
    static final String HOST_HEADER_NAME = "Host";
    static final String HTTP_HEADER_ACCEPT_ENCODING = "accept-encoding";
    static final String HTTP_HEADER_CONTENT_ENCODING = "content-encoding";
    static final String HTTP_HEADER_TRANSFER_ENCODING = "transfer-encoding";
    static final int DEFAULT_PROXY_PORT = 80;

    /**
     * Default private constructor to enforce singleton.
     */
    private Utils() {
    }

    /**
     * @param ch
     * @return
     */
    final static int escapeHtmlFull(int ch) {
        if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
            // safe
            return ch;
        } else if (Character.isWhitespace(ch)) {
            if (ch != '\n' && ch != '\r' && ch != '\t')
                // safe
                return ch;
        } else if (Character.isDefined(ch)) {
            // safe
            return ch;
        } else if (Character.isISOControl(ch)) {
            // paranoid version:isISOControl which are not isWhitespace
            // removed !
            // do nothing do not include in output !
            return -1;
        } else if (Character.isHighSurrogate((char) ch)) {
            // do nothing do not include in output !
            return -1;
        } else if (Character.isLowSurrogate((char) ch)) {
            // wrong char[] sequence, //TODO: LOG !!!
            return -1;
        }
    
        return -1;
    }

    /**
     * @param proxyInfo 
     * @return String
     */
    static final  String getProxyHostAndPort(ProxyInfo proxyInfo) {
        if (proxyInfo.getProxyPort() == 80) {
            return proxyInfo.getProxyHost();
        } else {
            return proxyInfo.getProxyHost() + ":" + proxyInfo.getProxyPort();
        }
    }

}
