FROM maven:3-eclipse-temurin-17-alpine AS build
COPY . /opt/
RUN mvn -ntp -f /opt/pom.xml -s /opt/settings.xml clean package

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /opt/target/contract-orchestrator-*.jar /opt/contract-orchestrator.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/contract-orchestrator.jar"]
