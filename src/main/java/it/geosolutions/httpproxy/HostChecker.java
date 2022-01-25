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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpRequestBase;

/**
 * HostChecker class for host check.
 * 
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 */
public class HostChecker implements ProxyCallback {

    ProxyConfig config;

    /**
     * @param config
     */
    public HostChecker(ProxyConfig config) {
        this.config = config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void onRequest(HttpServletRequest request, HttpServletResponse response, URL url)
            throws IOException {
        Set<String> hosts = config.getHostsWhitelist();

        // ////////////////////////////////
        // Check the whitelist of hosts
        // ////////////////////////////////

        if (hosts != null && hosts.size() > 0) {
            String host = getRemoteAddr(request);

            if (!hosts.contains(host)) {
                throw new HttpErrorException(403, "Client Host " + host
                        + " is not among the ones allowed for this proxy");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onRemoteResponse(org.apache.commons.httpclient.HttpMethod)
     */
    public void onRemoteResponse(HttpRequestBase method) throws IOException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.geosolutions.httpproxy.ProxyCallback#onFinish()
     */
    public void onFinish() throws IOException {
    }

    /**
     * @param req
     * @return String
     */
    private String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            String[] ips = forwardedFor.split(", ");
            return ips[0];
        } else {
            return req.getRemoteAddr();
        }
    }

}
