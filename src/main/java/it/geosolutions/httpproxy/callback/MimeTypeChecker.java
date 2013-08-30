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
package it.geosolutions.httpproxy.callback;

import it.geosolutions.httpproxy.exception.HttpErrorException;
import it.geosolutions.httpproxy.service.ProxyConfig;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethod;

/**
 * MimeTypeChecker class for the mimetype check.
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Alejandro Diaz
 * 
 */
public class MimeTypeChecker extends AbstractProxyCallback implements ProxyCallback {

    /**
     * Default constructor
     */
    public MimeTypeChecker() {
    	super();
    }

    /**
     * @param config
     */
    public MimeTypeChecker(ProxyConfig config) {
        super(config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void onRequest(HttpServletRequest request, HttpServletResponse response, URL url)
            throws IOException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onRemoteResponse(org.apache.commons.httpclient.HttpMethod)
     */
    public void onRemoteResponse(HttpMethod method) throws IOException {
        Set<String> mimeTypes = config.getMimetypeWhitelist();

        if (mimeTypes != null && mimeTypes.size() > 0) {
            String contentType = method.getResponseHeader("Content-type").getValue();

            // //////////////////////////////////
            // Trim off extraneous information
            // //////////////////////////////////

            String firstType = contentType.split(";")[0];

            if (!mimeTypes.contains(firstType)) {
                throw new HttpErrorException(403, "Content-type " + firstType
                        + " is not among the ones allowed for this proxy");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onFinish()
     */
    public void onFinish() throws IOException {
    }

}
