# Container with application
FROM bellsoft/liberica-openjre-alpine:17.0.5
RUN apk --no-cache add curl
COPY /build/install/ddns /ddns
ENTRYPOINT /ddns/bin/ddns
