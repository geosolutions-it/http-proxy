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
package it.geosolutions.httpproxy;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 */
final class Utils {

    static final int DEFAULT_MAX_FILE_UPLOAD_SIZE = 5 * 1024 * 1024;

    /**
     * The directory to use to temporarily store uploaded files
     */
    static final File DEFAULT_FILE_UPLOAD_TEMP_DIRECTORY = new File(
            System.getProperty("java.io.tmpdir"));

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
    
    static final String HTTP_HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    static final int DEFAULT_PROXY_PORT = 80;

    static final int DEFAULT_HTTPS_PORT = 443;

    static final int DEFAULT_HTTP_PORT = 80;

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
    static final String getProxyHostAndPort(ProxyInfo proxyInfo) {
        if (proxyInfo.getProxyPort() == 80) {
            return proxyInfo.getProxyHost();
        } else {
            return proxyInfo.getProxyHost() + ":" + proxyInfo.getProxyPort();
        }
    }

    /**
     * @param property
     * @return Set<String>
     */
    static final Set<String> parseWhiteList(String property) {
        if (property != null) {
            Set<String> set = new HashSet<String>();

            String[] array = property.split(",");

            for (int i = 0; i < array.length; i++) {
                String element = array[i];
                if (element != null)
                    set.add(element);
            }

            return set;
        } else {
            return null;
        }
    }

    static URL buildURL(String value) throws MalformedURLException {
        URL url = new URL(value);

        if(url.getPort() == -1) {
            if(url.toString().startsWith("https://")) {
                return new URL(url.getProtocol(), url.getHost(), DEFAULT_HTTPS_PORT, url.getFile());
            } else {
                return new URL(url.getProtocol(), url.getHost(), DEFAULT_HTTP_PORT, url.getFile());
            }
        }

        return url;
    }
}
