FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/${artifactId}-${version}.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
