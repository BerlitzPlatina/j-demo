# Expects the jar to be built first: ./mvnw -f gateway/pom.xml package
# Kept as a plain copy rather than a multi-stage build so an image rebuild does not re-resolve
# every Maven dependency inside docker-in-docker.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
