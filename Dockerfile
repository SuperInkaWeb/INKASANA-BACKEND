# --- Etapa 1: Compilar con Maven + JDK 21 ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiamos primero el wrapper y el pom.xml para aprovechar la cache de Docker
COPY mvnw ./
COPY .mvn ./.mvn
COPY pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Ahora copiamos el resto del código y compilamos
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# --- Etapa 2: Imagen final, solo con el JAR ya compilado ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Render/Railway inyectan el puerto real en la variable PORT.
# Si no viene, usamos 8080 por defecto (coincide con application.yaml).
ENV PORT=8080
EXPOSE 8080

# -Dserver.port=${PORT} permite que la plataforma controle el puerto sin tocar application.yaml
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
