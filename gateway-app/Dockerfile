FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/ian-ddd-gateway-1.0-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
