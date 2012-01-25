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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethod;

public class MimeTypeChecker implements ProxyCallback {

    ProxyConfig config;

    public MimeTypeChecker(ProxyConfig config) {
        this.config = config;
    }

    public void onRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // nothing to do
    }

    public void onRemoteResponse(HttpMethod method) throws IOException {
        Set<String> mimeTypes = config.getMimetypeWhitelist();
        if (mimeTypes != null && mimeTypes.size() > 0) {
            String contentType = method.getResponseHeader("Content-type").getValue();
            if (!mimeTypes.contains(contentType)) {
                throw new HttpErrorException(403, "Content-type " + contentType
                        + " is not among the ones allowed for this proxy");
            }
        }

    }

    public void onFinish() throws IOException {
        // TODO Auto-generated method stub

    }

}
