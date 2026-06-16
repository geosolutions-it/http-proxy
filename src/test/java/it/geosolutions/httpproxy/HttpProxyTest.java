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
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * HttpProxyTest class. Test Cases for the HTTPProxy servlet.
 *
 * @author Lorenzo Natali at lorenzo.natali@geo-solutions.it
 */
public class HttpProxyTest {

    final ServletConfig servletConfig = mock(ServletConfig.class);
    ServletContext ctx = mock(ServletContext.class);
    Map<String, String[]> parameters = new HashMap<>();
    private List<String> headers = new ArrayList<>();

    CloseableHttpClient mockHttpClient;
    HTTPProxy proxy;
    String fakeLocation;

    @BeforeEach
    public void setUp() {
        File f = new File(getClass().getClassLoader()
                .getResource("test-proxy.properties").getFile());
        when(ctx.getInitParameter("proxyPropPath")).thenReturn(
                f.getAbsolutePath());
        when(servletConfig.getServletContext()).thenReturn(ctx);

        // setup base parameters
        parameters.put("url", new String[]{"http://sample.com/"});

    }

    @Test
    public void testRedirectGet() throws Exception {

        // mock redirect response
        final HttpGet mockGetMethod = mock(HttpGet.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getCode()).thenReturn(302);

        mockHttpClient = mock(CloseableHttpClient.class);
        when(mockHttpClient.execute(mockGetMethod)).thenReturn(response);
        fakeLocation = "http://newURL.com/";

        when(mockGetMethod.getFirstHeader(Utils.LOCATION_HEADER))
                .thenReturn(new BasicHeader("Location", fakeLocation));

        proxy = new HTTPProxy() {
            private static final long serialVersionUID = 1L;

            @Override
            public HttpGet getGetMethod(URL url) {
                return mockGetMethod;
            }
        };
        proxy.setHttpClient(mockHttpClient);
        proxy.init(servletConfig);

        // mock http request to proxy
        HttpServletRequest getRequest = mock(HttpServletRequest.class);
        when(getRequest.getParameterMap()).thenReturn(parameters);
        when(getRequest.getHeaderNames()).thenReturn(
                Collections.enumeration(headers));
        when(getRequest.getRequestURL()).thenReturn(
                new StringBuffer("http://proxy.com/http-proxy/proxy"));
        // mock http response object
        HttpServletResponse getResponse = mock(HttpServletResponse.class);
        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
        when(getResponse.getOutputStream()).thenReturn(servletOutputStream);

        proxy.doGet(getRequest, getResponse);
        verify(getResponse).sendRedirect(
                "http://proxy.com/http-proxy/proxy?url="
                        + URLEncoder.encode(fakeLocation, "UTF-8"));
        final byte[] data = servletOutputStream.baos.toByteArray();
        Assertions.assertNotNull(data);
        Assertions.assertTrue(data.length == 0);
    }

    @Test
    public void testPost() throws Exception {

        // mock post response
        final HttpPost mockPostMethod = mock(HttpPost.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getCode()).thenReturn(200);

        HttpEntity stringEntity = new StringEntity("user created");
        when(response.getEntity()).thenReturn(stringEntity);
        when(response.headerIterator()).thenReturn(Collections.<org.apache.hc.core5.http.Header>emptyList().iterator());
        mockHttpClient = mock(CloseableHttpClient.class);
        when(mockHttpClient.execute(mockPostMethod)).thenReturn(response);

        proxy = new HTTPProxy() {
            private static final long serialVersionUID = 1L;

            @Override
            public HttpPost getPostMethod(URL url) {
                return mockPostMethod;
            }
        };
        proxy.setHttpClient(mockHttpClient);
        proxy.init(servletConfig);

        // mock http request to proxy
        HttpServletRequest postRequest = mock(HttpServletRequest.class);
        when(postRequest.getQueryString()).thenReturn("url=https://jsonplaceholder.typicode.com/test/createUser");
        when(postRequest.getMethod()).thenReturn("post");

        Enumeration<String> enumeration = Collections.enumeration(Collections.emptyList());
        when(postRequest.getHeaderNames()).thenReturn(enumeration);

        ServletInputStream stream = mock(ServletInputStream.class);
        when(postRequest.getInputStream()).thenReturn(stream);

        // mock http response object
        HttpServletResponse getResponse = mock(HttpServletResponse.class);
        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
        when(getResponse.getOutputStream()).thenReturn(servletOutputStream);

        proxy.doPost(postRequest, getResponse);
        verify(getResponse).setStatus(200);
        final byte[] data = servletOutputStream.baos.toByteArray();
        Assertions.assertNotNull(data);
        Assertions.assertTrue(data.length != 0);
    }

}
