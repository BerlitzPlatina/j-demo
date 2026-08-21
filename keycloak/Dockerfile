# Expects the jar to be built first: ./mvnw -f keycloak/pom.xml package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/keycloak-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
