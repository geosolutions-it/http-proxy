package it.geosolutions.httpproxy;

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

    /**
     * Constructs a FilteredHeaderRequestWrapper instance.
     *
     * @param request          The original HttpServletRequest object to be wrapped.
     * @param disallowedHeaders A set containing header names to be filtered out (removed).
     */
    public FilteredHeaderRequestWrapper(HttpServletRequest request, Set<String> disallowedHeaders) {
        super(request);
        this.disallowedHeaders = disallowedHeaders;
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
        if (disallowedHeaders.contains(name.toLowerCase())) {
            return null; // Return null to remove the header
        }
        return super.getHeader(name);
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
        if (disallowedHeaders.contains(name.toLowerCase())) {
            return Collections.emptyEnumeration(); // Return empty enumeration to remove the header
        }
        return super.getHeaders(name);
    }
}
