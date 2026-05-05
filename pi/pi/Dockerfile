FROM eclipse-temurin:17-jre
WORKDIR /app

ENV JAVA_OPTS=""
ENV SERVER_PORT=8091

COPY target/*.jar /app/app.jar

EXPOSE 8091

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
