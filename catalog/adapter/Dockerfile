# Catalog service image. The jar is built on the host (`mvn package` — msfw deps live only in the
# local ~/.m2, so we don't build inside the image) and copied in. arm64-native JRE 21.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/catalog-service-1.0-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=k8s
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
