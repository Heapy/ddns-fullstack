# Container with application
FROM bellsoft/liberica-openjre-alpine:21.0.5
RUN apk --no-cache add curl
COPY /build/install/ddns /ddns
ENTRYPOINT /ddns/bin/ddns
