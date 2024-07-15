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

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;

/**
 * RequestTypeChecker class for request type check.
 * 
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 */
public class RequestTypeChecker implements ProxyCallback {

    ProxyConfig config;

    /**
     * @param config
     */
    public RequestTypeChecker(ProxyConfig config) {
        this.config = config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public HttpServletRequest onRequest(HttpServletRequest request, HttpServletResponse response, URL url)
            throws IOException {
        Set<String> reqTypes = config.getReqtypeWhitelist();

        // //////////////////////////////////////
        // Check off the request type
        // provided vs. permitted request types
        // //////////////////////////////////////

        if (reqTypes != null && reqTypes.size() > 0) {
            Iterator<String> iterator = reqTypes.iterator();

            String urlExtForm = url.toExternalForm();
            /*if (urlExtForm.indexOf("?") != -1) {
                urlExtForm = urlExtForm.split("\\?")[1];
            }*/

            boolean check = false;
            while (iterator.hasNext()) {
                String regex = iterator.next();

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(urlExtForm);

                if (matcher.matches()) {
                    check = true;
                    break;
                }
            }

            if (!check)
                throw new HttpErrorException(403, "Request Type"
                        + " is not among the ones allowed for this proxy");
        }
        return request;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onRemoteResponse(org.apache.commons.httpclient.HttpMethod)
     */
    public void onRemoteResponse(HttpResponse response) throws IOException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onFinish()
     */
    public void onFinish() throws IOException {
    }

}
