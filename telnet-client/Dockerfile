FROM adoptopenjdk/openjdk8:alpine-slim
COPY build/libs/*SNAPSHOT.jar telnet-client.jar
ENTRYPOINT ["java", "-jar", "/telnet-client.jar"]
