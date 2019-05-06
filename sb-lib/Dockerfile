FROM openjdk:11-slim
COPY build/libs/sb-lib-1.0-all.jar /servicebus/demo/sb-lib-1.0-all.jar

ENTRYPOINT ["/usr/bin/java", "-jar", "/servicebus/demo/sb-lib-1.0-all.jar"]