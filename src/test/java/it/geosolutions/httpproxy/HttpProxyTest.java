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

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * HttpProxyTest class. Test Cases for the HTTPProxy servlet.
 * 
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 */
public class HttpProxyTest extends BaseHttpTest {

    @Test
    public void testDoGet() throws Exception {

        // ////////////////////////////
        // Test with a correct request
        // ////////////////////////////

        URL url = new URL("http://localhost:8080/http_proxy/proxy/?"
                + "url=http%3A%2F%2Fdemo1.geo-solutions.it%2Fgeoserver%2Fwms%3F"
                + "SERVICE%3DWMS%26REQUEST%3DGetCapabilities%26version=1.1.1");
        
        // Original response
        URL urlWithoutProxy = new URL("http://demo1.geo-solutions.it/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities&version=1.1.1");
        HttpURLConnection conWithoutProxy = (HttpURLConnection) urlWithoutProxy.openConnection();
        String responseWithoutProxy = IOUtils.toString(conWithoutProxy.getInputStream());
        
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        String response = IOUtils.toString(con.getInputStream());

        assertNotNull(response);
        assertEquals(response, responseWithoutProxy);
        assertTrue(con.getRequestMethod().equals("GET"));
        assertTrue(con.getResponseCode() == 200);

        con.disconnect();
        conWithoutProxy.disconnect();

        // ////////////////////////////
        // Test with a fake hostname
        // ////////////////////////////

        url = new URL("http://localhost:8080/http_proxy/proxy/?"
                + "url=http%3A%2F%2FfakeServer%2Fgeoserver%2Fwms%3F"
                + "SERVICE%3DWMS%26REQUEST%3DGetCapabilities%26version=1.1.1");

        con = (HttpURLConnection) url.openConnection();

        String message = con.getResponseMessage();

        assertNotNull(message);
        assertEquals(message, "Host Name fakeServer is not among the ones allowed for this proxy");

        assertTrue(con.getRequestMethod().equals("GET"));
        assertTrue(con.getResponseCode() == 403);

        con.disconnect();

        // ///////////////////////////////
        // Test with a fake request type
        // ///////////////////////////////

        url = new URL("http://localhost:8080/http_proxy/proxy/?"
                + "url=http%3A%2F%2Fdemo1.geo-solutions.it%2Fgeoserver%2Fwms%3F"
                + "SERVICE%3DWMS%26REQUEST%3DGetCap%26version=1.1.1");

        con = (HttpURLConnection) url.openConnection();

        message = con.getResponseMessage();

        assertNotNull(message);
        assertEquals(message, "Request Type is not among the ones allowed for this proxy");

        assertTrue(con.getRequestMethod().equals("GET"));
        assertTrue(con.getResponseCode() == 403);

        con.disconnect();
    }

}