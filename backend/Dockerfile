# Build em multiplos estagios: o estagio final nao carrega Maven nem o
# codigo-fonte, so o JAR e um JRE. A imagem cai de ~700MB para ~250MB,
# e menos coisa dentro significa menos superficie de ataque.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copia so o pom primeiro: se as dependencias nao mudaram, o Docker
# reaproveita esta camada e pula o download inteiro no proximo build.
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Usuario sem privilegio: se alguem escapar da aplicacao, nao e root.
RUN addgroup -S cofre && adduser -S cofre -G cofre
COPY --from=build /app/target/*.jar app.jar
USER cofre
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
