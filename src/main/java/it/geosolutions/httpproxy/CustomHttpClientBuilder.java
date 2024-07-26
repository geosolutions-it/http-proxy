package it.geosolutions.httpproxy;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.protocol.HttpContext;

import java.util.HashSet;
import java.util.Set;

public class CustomHttpClientBuilder {

    public static HttpClientBuilder create(ProxyConfig proxyConfig) {
        Set<String> cookiesToSkip = proxyConfig != null ? proxyConfig.getCookiesToSkip() : new HashSet<>();
        return HttpClientBuilder.create()
                .setDefaultCookieSpecRegistry(createCookieSpecRegistry(cookiesToSkip));
    }

    private static Registry<CookieSpecProvider> createCookieSpecRegistry(Set<String> cookiesToSkip) {
        return RegistryBuilder.<CookieSpecProvider>create()
                .register("custom", new CookieSpecProvider() {
                    @Override
                    public CookieSpec create(HttpContext context) {
                        return new DefaultCookieSpec() {
                            @Override
                            public boolean match(Cookie cookie, CookieOrigin origin) {
                                if (cookiesToSkip.contains(cookie.getName().toLowerCase())) {
                                    return false;
                                }
                                return super.match(cookie, origin);
                            }
                        };
                    }
                })
                .build();
    }
}