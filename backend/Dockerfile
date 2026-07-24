# Etapa 1: compila el backend (no hace falta tener Java ni Maven instalados fuera del contenedor)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# Etapa 2: imagen final, liviana, solo con el JRE y el .jar ya compilado
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/siga-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
