# Define a imagem base
FROM openjdk:17-jdk-slim

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia o arquivo JAR para o diretório de trabalho no contêiner
COPY target/*.jar app.jar

# Define o comando padrão a ser executado quando o contêiner for iniciado
CMD java $JVM_OPTIONS -jar app.jar