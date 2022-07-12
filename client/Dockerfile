FROM java:8
FROM maven:alpine

# image layer
WORKDIR /app
ADD pom.xml /app
RUN mvn verify clean --fail-never

# Image layer: with the application
COPY . /app
RUN mvn -v
RUN mvn clean install -DskipTests
ADD ./target/client-emulator-1.0.0-jar-with-dependencies.jar /developments/
ENTRYPOINT ["java","-jar","/developments/client-emulator-1.0.0-jar-with-dependencies.jar"]