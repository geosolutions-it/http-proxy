package it.geosolutions.httpproxy;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

/**
 * AuthorizationHeadersChecker class for the authorization check.
 *
 * @author Alessio Fabiani - GeoSolutions
 *
 */
public class AuthorizationHeadersChecker implements ProxyCallback {

    ProxyConfig config;

    /**
     * @param config
     */
    public AuthorizationHeadersChecker(ProxyConfig config) {
        this.config = config;
    }

    /**
     * First to be called, can be used to initialize the callback status and disallow certain requests by throwing an {@link HttpErrorException}
     *
     * @param request
     * @param response
     * @param url
     * @return
     * @throws IOException
     */
    @Override
    public HttpServletRequest onRequest(HttpServletRequest request, HttpServletResponse response, URL url) throws IOException {
        return new FilteredHeaderRequestWrapper(request, config.getDisallowedAuthHeaders());
    }

    /**
     * Second to be called, can be used to check the remote server response
     *
     * @param method
     * @throws IOException
     */
    @Override
    public void onRemoteResponse(HttpResponse response) throws IOException {
        Set<String> disallowedAuthHeaders = config.getDisallowedAuthHeaders();

        if (disallowedAuthHeaders != null && !disallowedAuthHeaders.isEmpty()) {
            // Copy headers from remote response to the client response, excluding disallowed headers
            Header[] headers = response.getAllHeaders();
            if (headers != null) {
                for (Header header : headers) {
                    String headerName = header.getName().toLowerCase();
                    if (disallowedAuthHeaders.contains(headerName)) {
                        response.removeHeader(header);
                    }
                }
            }
        }
    }

    /**
     * Called when the request is fully proxied, can be used for cleanup actions
     *
     * @throws IOException
     */
    @Override
    public void onFinish() throws IOException {

    }
}
