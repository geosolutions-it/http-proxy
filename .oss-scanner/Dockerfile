# Build environment for the Anthropic OSS Scanner (https://github.com/anthropics/oss-scanner).
# The scanner builds this file with the repository as context and then runs the image without network,
# so every Maven artifact needed to build, test and run the proxy is downloaded here.
FROM maven:3.9-eclipse-temurin-17

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl git python3 \
    && rm -rf /var/lib/apt/lists/*

# scanner contract: the checkout lives inside the image, at /src
COPY . /src
WORKDIR /src

# Library and war, as CI builds them. Tests run here so the surefire providers are cached for offline runs;
# a failing test is reported but does not fail the image.
RUN mvn -B -ntp install -Dmaven.test.failure.ignore=true \
    && mvn -B -ntp dependency:resolve-plugins dependency:resolve

# Standalone proxy (embedded Jetty, /proxy/* on $PORT) built with the docker profile. Kept outside target/
# so a later `mvn clean` does not remove it. The final offline install rebuilds the plain artifacts, so
# target/ and the local repository hold the library and war again, not the shaded jar.
RUN mvn -B -ntp -Pdocker -DskipTests package \
    && mkdir -p /opt/http-proxy \
    && cp target/http-proxy.jar /opt/http-proxy/http-proxy.jar \
    && cp src/main/resources/proxy.properties /opt/http-proxy/proxy.properties \
    && mvn -B -ntp -o -q install -DskipTests

ENV PROXY_PROP_PATH=/opt/http-proxy/proxy.properties

# Maven offline mode refuses artifacts whose _remote.repositories marker names a repository other than the
# one it would ask now. Without network every cached artifact is valid, so drop the markers.
RUN find /root/.m2/repository -name _remote.repositories -delete
