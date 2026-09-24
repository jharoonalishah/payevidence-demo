# PayEvidence demo image (H2 file DB). Build from payevidence/ root:
#   docker build -f app/Dockerfile -t payevidence:0.6 .
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY app/pom.xml ./app/pom.xml
COPY app/src ./app/src
WORKDIR /build/app
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN adduser -D -u 10001 payevidence
COPY --from=build /build/app/target/payevidence-*.jar /app/app.jar
COPY day1 /data/day1
COPY day2-3 /data/day2-3
RUN mkdir -p /app/data /app/packs && chown -R payevidence:payevidence /app
USER payevidence
ENV JAVA_OPTS="" \
    PAYEVIDENCE_CATALOGUE=/data/day1/controls/pci-mvp-catalogue.yaml \
    PAYEVIDENCE_SAMPLES=/data/day1/samples \
    PAYEVIDENCE_CHANGE_RECORD=/data/day2-3/samples/change_record.json \
    PAYEVIDENCE_PACKS=/app/packs \
    PAYEVIDENCE_DEMO_TOKEN=nordic-demo \
    H2_CONSOLE_ENABLED=false \
    PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
