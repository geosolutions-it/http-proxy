package it.geosolutions.httpproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

/**
 * A wrapper class for HttpServletRequest that filters out specified headers.
 * Headers listed in the disallowedHeaders set will be removed from the request.
 */
public class FilteredHeaderRequestWrapper extends HttpServletRequestWrapper {
    private Set<String> disallowedHeaders;
    private static final Logger LOGGER = LoggerFactory.getLogger(FilteredHeaderRequestWrapper.class);

    /**
     * Constructs a FilteredHeaderRequestWrapper instance.
     *
     * @param request          The original HttpServletRequest object to be wrapped.
     * @param disallowedHeaders A set containing header names to be filtered out (removed).
     */
    public FilteredHeaderRequestWrapper(HttpServletRequest request, Set<String> disallowedHeaders) {
        super(request);
        this.disallowedHeaders = disallowedHeaders;
        LOGGER.debug("FilteredHeaderRequestWrapper created with {} disallowed headers", disallowedHeaders.size());
    }

    /**
     * Returns the value of the specified request header.
     * If the header is in the disallowedHeaders set, returns null to remove the header.
     *
     * @param name The name of the header.
     * @return The value of the header, or null if the header should be removed.
     */
    @Override
    public String getHeader(String name) {
        LOGGER.debug("Getting header: {}", name);
        if (disallowedHeaders.contains(name.toLowerCase())) {
            LOGGER.debug("Header '{}' is disallowed, returning null", name);
            return null; // Return null to remove the header
        }
        String headerValue = super.getHeader(name);
        LOGGER.debug("Header '{}' value: {}", name, headerValue);
        return headerValue;
    }

    /**
     * Returns an enumeration of all the header names this request contains.
     * If the header is in the disallowedHeaders set, returns an empty enumeration to remove the header.
     *
     * @param name The name of the header.
     * @return An enumeration of the header values, or an empty enumeration if the header should be removed.
     */
    @Override
    public Enumeration<String> getHeaders(String name) {
        LOGGER.debug("Getting headers for: {}", name);
        if (disallowedHeaders.contains(name.toLowerCase())) {
            LOGGER.debug("Header '{}' is disallowed, returning empty enumeration", name);
            return Collections.emptyEnumeration(); // Return empty enumeration to remove the header
        }
        Enumeration<String> headers = super.getHeaders(name);
        LOGGER.debug("Retrieved headers for '{}'. Has elements: {}", name, headers.hasMoreElements());
        return headers;
    }
}