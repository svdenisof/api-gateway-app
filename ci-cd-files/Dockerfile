FROM 192.168.1.49:5000/library/jdk/bellsoft-liberica-openjdk-alpine-musl:21.0.6
WORKDIR "/u/app"

RUN apk update
RUN apk upgrade --no-cache
RUN apk add -f libfreetype6;
RUN apk add -f fontconfig;
RUN apk add -f ttf-dejavu;
RUN apk add -f ca-certificates;

RUN rm -rf /etc/localtime
RUN ln -s /usr/share/zoneinfo/Europe/Moscow /etc/localtime
RUN addgroup -S appuser && adduser -S appuser -G appuser

COPY --chown=appuser:appuser target/*.jar /u/app/api-gateway-applicaton.jar

ENV JAVA_OPTS="-XX:+UseCompressedOops -XX:+UseG1GC"
ENV JAVA_OPTS="$JAVA_OPTS -Dhttp.nonProxyHosts=127.0.0.1|localhost"

USER appuser
CMD java -jar $JAVA_OPTS /u/app/api-gateway-applicaton.jar
