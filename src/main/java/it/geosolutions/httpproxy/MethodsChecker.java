package it.geosolutions.httpproxy;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MethodsChecker class for http methods check.
 *
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 */
public class MethodsChecker implements ProxyCallback {
    private final ProxyConfig config;
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodsChecker.class);

    /**
     * @param config The proxy configuration
     */
    public MethodsChecker(ProxyConfig config) {
        this.config = config;
        LOGGER.debug("MethodsChecker initialized with config: {}", config);
    }

    /*
     * (non-Javadoc)
     *
     * @see it.geosolutions.httpproxy.ProxyCallback#onRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public HttpServletRequest onRequest(HttpServletRequest request, HttpServletResponse response, URL url)
            throws IOException {
        LOGGER.debug("Checking request method for URL: {}", url);

        Set<String> methods = config.getMethodsWhitelist();

        // ////////////////////////////////
        // Check the allowlist of methods
        // ////////////////////////////////
        if (methods != null && !methods.isEmpty()) {
            LOGGER.debug("Allowed methods: {}", methods);
            String method = request.getMethod();
            LOGGER.debug("Request method: {}", method);

            if (!methods.contains(method)) {
                LOGGER.warn("HTTP Method {} is not allowed for this proxy", method);
                throw new HttpErrorException(403, "HTTP Method " + method
                        + " is not among the ones allowed for this proxy");
            }
            LOGGER.debug("Method {} is allowed", method);
        } else {
            LOGGER.debug("No method restrictions configured");
        }

        return request;
    }

    /*
     * (non-Javadoc)
     *
     * @see it.geosolutions.httpproxy.ProxyCallback#onRemoteResponse(org.apache.commons.httpclient.HttpMethod)
     */
    public void onRemoteResponse(HttpResponse response) throws IOException {
        LOGGER.debug("Received remote response with status: {}", response.getStatusLine().getStatusCode());
    }

    /*
     * (non-Javadoc)
     *
     * @see it.geosolutions.httpproxy.ProxyCallback#onFinish()
     */
    public void onFinish() throws IOException {
        LOGGER.debug("MethodsChecker finished processing");
    }
}