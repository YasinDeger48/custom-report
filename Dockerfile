FROM mongo:6.0

# Opsiyonel: Veritabanı, kullanıcı vs. ön tanımlı oluşturmak için init script koyabilirsin
# COPY ./init.js /docker-entrypoint-initdb.d/

EXPOSE 27017
