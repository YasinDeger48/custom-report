FROM openjdk:17-jdk-slim

WORKDIR /app

COPY dashboard/target/dashboard.jar app.jar

EXPOSE 6666

ENTRYPOINT ["java", "-jar", "app.jar"]
