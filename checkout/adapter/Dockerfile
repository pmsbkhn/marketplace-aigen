# Checkout service image. Host-built fat jar copied onto an arm64-native JRE 21.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/checkout-service-1.0-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=k8s
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
