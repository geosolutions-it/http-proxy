package it.geosolutions.httpproxy;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

public class AuthorizationHeadersChecker implements ProxyCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationHeadersChecker.class);

    private ProxyConfig config;

    public AuthorizationHeadersChecker(ProxyConfig config) {
        this.config = config;
        LOGGER.debug("AuthorizationHeadersChecker initialized with config: {}", config);
    }

    @Override
    public HttpServletRequest onRequest(HttpServletRequest request, HttpServletResponse response, URL url) throws IOException {
        LOGGER.debug("onRequest called for URL: {}", url);

        Set<String> disallowedHeaders = config.getDisallowedAuthHeaders();
        LOGGER.debug("Disallowed headers: {}", disallowedHeaders);

        LOGGER.debug("Original request headers:");
        dumpHeaders(request);

        HttpServletRequest filteredRequest = new FilteredHeaderRequestWrapper(request, disallowedHeaders);

        LOGGER.debug("Filtered request headers:");
        dumpHeaders(filteredRequest);

        return filteredRequest;
    }

    @Override
    public void onRemoteResponse(HttpResponse response) throws IOException {
        LOGGER.debug("onRemoteResponse called");

        Set<String> disallowedAuthHeaders = config.getDisallowedAuthHeaders();
        LOGGER.debug("Disallowed auth headers: {}", disallowedAuthHeaders);

        LOGGER.debug("Original remote response headers:");
        dumpHeaders(response);

        if (disallowedAuthHeaders != null && !disallowedAuthHeaders.isEmpty()) {
            Header[] headers = response.getAllHeaders();
            if (headers != null) {
                for (Header header : headers) {
                    String headerName = header.getName().toLowerCase();
                    if (disallowedAuthHeaders.contains(headerName)) {
                        LOGGER.debug("Removing disallowed header: {}", header.getName());
                        response.removeHeader(header);
                    }
                }
            }
        }

        LOGGER.debug("Filtered remote response headers:");
        dumpHeaders(response);
    }

    @Override
    public void onFinish() throws IOException {
        LOGGER.debug("onFinish called");
    }

    private void dumpHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                LOGGER.debug("Header: {} = {}", name, request.getHeader(name));
            }
        }
    }

    private void dumpHeaders(HttpResponse response) {
        if (response.getAllHeaders() != null) {
            for (Header header : response.getAllHeaders()) {
                LOGGER.debug("Header: {} = {}", header.getName(), header.getValue());
            }
        }
    }
}