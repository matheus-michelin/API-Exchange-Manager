FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia dependências primeiro para aproveitar o cache do Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código fonte e faz o empacotamento pulando os testes
COPY src ./src
RUN mvn clean package -DskipTests

# --- ETAPA 2: Execução da aplicação ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copia o .jar gerado na etapa anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]