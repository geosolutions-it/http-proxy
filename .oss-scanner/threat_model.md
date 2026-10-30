# Threat model

The scanner reads this file before it starts. It covers the GeoSolutions HTTP Proxy on the `master`
branch. Our disclosure policy is in [SECURITY.md](../SECURITY.md).

## What this project does and where untrusted input enters

The HTTP Proxy is a small Java servlet (`it.geosolutions.httpproxy.HTTPProxy`) that forwards browser
requests to remote OGC services (WMS, WFS, WCS, CSW, ...) to work around CORS. Its main user is
[MapStore](https://github.com/geosolutions-it/MapStore2), which maps it at `/proxy/*` and calls it as
`/proxy/?url=<target>`. It also ships as a standalone fat jar (`Main`, embedded Jetty, `-Pdocker`).

The proxy is normally reachable **without authentication** from the Internet: any visitor of a MapStore
instance can call it. Untrusted input:

- **The whole incoming request**: the `url` parameter, the path, the query string, the method, headers
  (including `Host`, `Cookie`, `Authorization`, `X-Forwarded-*`) and the body.
- **The remote server's response**: status, headers (`Location`, `Content-Type`, `Set-Cookie`,
  `Content-Disposition`, ...) and body. The target may be chosen by the attacker, so the response is
  attacker-controlled too, and it is served back under the origin of the MapStore instance.
- **DNS answers** for the target host name.

Trusted: `proxy.properties` (path from the `proxy.properties` init parameter or `PROXY_PROP_PATH`), the
servlet container configuration, JVM system properties such as `http.proxyHost`.

### Security goals

1. **Destination control.** The proxy reaches only the destinations the configuration allows. By default
   it refuses non-public addresses (loopback, private, link-local, cloud metadata, ...) unless they are in
   `allowedPrivateHosts` / `allowedPrivateNetworks`, and it applies `hostnameWhitelist` when set. Bypasses
   through redirects, DNS rebinding, alternative IP notations, IPv6 forms, userinfo, encoded characters or
   the outbound JVM proxy are in scope.
2. **Request filtering.** Method, request type (`reqtypeWhitelist.*`) and MIME type checks
   (`MethodsChecker`, `RequestTypeChecker`, `MimeTypeChecker`, `HostChecker`, `HostNameChecker`) cannot be
   bypassed.
3. **Response isolation.** A proxied response must not run script in the MapStore origin: responses carry a
   sandboxing `Content-Security-Policy` and `nosniff`. Ways around this (header injection, response
   splitting, a content type or disposition that escapes the sandbox) are in scope.
4. **Cookie isolation.** The browser's cookies belong to the MapStore origin and are not forwarded
   (default request header blacklist: `Cookie`); the remote server cannot set or clear state on the
   MapStore origin (default response header blacklist: `Set-Cookie`, `Set-Cookie2`, `Clear-Site-Data`,
   `Strict-Transport-Security`). One HTTP client serves all users, so nothing set by one user's target may
   be replayed for another user.
5. **No open redirect** from the proxy's own redirects (`Location` built from the servlet path).
6. **Availability.** Connection pool limits, timeouts and stream sizes (`max_total_connections`,
   `timeout`, `defaultStreamByteSize`) hold under hostile targets (slow or endless responses).

## Components that matter most / least

- Most: `HTTPProxy.java` (request handling, header copy, redirects, response writing),
  `PrivateAddressDnsResolver.java`, `HostChecker.java`, `HostNameChecker.java`, `Utils.java`,
  `ProxyConfig.java` (parsing of the lists).
- Less: `Main.java` (standalone launcher), `ProxyInfo`, logging.
- Out of scope: `src/test/`, the release workflows under `.github/`.

Working rules: a test that asserts a behaviour marks it as deliberate. Read the code and probe the running
instance: some defects (URL parsing, redirects, encoded paths, header handling by the container) only show
up by sending requests, so use the instance to explore as well as to confirm. Once a root cause has a
reproducer and a patch, write it up rather than listing variants.

## How to exercise it

- Unit tests: `cd /src && mvn -o test`.
- Standalone proxy: `PORT=8080 java -jar /opt/http-proxy/http-proxy.jar`, then
  `curl 'http://localhost:8080/proxy/?url=http://<target>/...'`. The configuration is
  `/opt/http-proxy/proxy.properties` (`PROXY_PROP_PATH`). Start a local `python3 -m http.server` as a
  target; remember that loopback targets are refused by default, which is the behavior to test.

## How we rate severity

Rate against a default deployment. Name the role the attacker starts with and what they gain.

- **Critical**: unauthenticated request to a non-public or explicitly denied destination with the response
  readable by the attacker (full-read SSRF, e.g. cloud metadata), or code execution on the server.
- **High**: SSRF bypass of the default destination checks without readable response (blind), script
  execution in the MapStore origin through a proxied response despite the sandboxing headers, leak of the
  MapStore origin's credentials or session to a third-party destination.
- **Medium**: bypass of `hostnameWhitelist`, method or request type filters toward public hosts; open redirect;
  remote server able to set cookies on the MapStore origin; denial of service of the proxy by a single
  client with little traffic.
- **Low**: information disclosure without security impact (versions, stack traces), issues that need a
  non-default configuration the documentation warns against.
- Behavior that the configuration explicitly allows is not a finding: `allowPrivateDestinations = true`,
  a private host in `allowedPrivateHosts`, `checkResponseContentType = false`, a destination reached through a
  JVM outbound proxy when `allowForwardProxy = true` and the name does not resolve locally.
- Generic DoS by volume of requests is out of scope.

## Anything to leave alone

- Do not report the fact that the proxy is unauthenticated: that is the design. Report what it lets an
  anonymous caller reach.
- Dependabot and CodeQL already scan this project: do not report dependency CVEs or plain CodeQL patterns
  unless you show they are reachable and exploitable. Focus on logic flaws: host and port checks, redirects,
  header and credential forwarding.
