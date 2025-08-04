# Temel imaj olarak Java 21 kullanıyoruz (veya uygun versiyon)
FROM eclipse-temurin:21-jre-alpine

# Çalışma dizini
WORKDIR /app

# Uygulama jar dosyasını kopyala
COPY target/dashboard-1.0-SNAPSHOT.jar app.jar

# Uygulamayı 6666 portunda çalıştır
EXPOSE 6666

# Uygulamayı çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
