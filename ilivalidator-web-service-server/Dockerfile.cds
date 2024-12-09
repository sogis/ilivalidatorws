FROM bellsoft/liberica-openjdk-debian:21.0.4-9-cds AS builder
WORKDIR /workspace/app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY ilivalidator-web-service-client ilivalidator-web-service-client
COPY ilivalidator-web-service-shared ilivalidator-web-service-shared
COPY ilivalidator-web-service-server ilivalidator-web-service-server
COPY .git .git
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DexcludedGroups="docker" -DskipTests

FROM bellsoft/liberica-openjdk-debian:21.0.4-9-cds AS optimizer
WORKDIR /workspace/app
COPY --from=builder /workspace/app/ilivalidator-web-service-server/target/ilivalidator-web-service-server-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --destination application
WORKDIR /workspace/app/application
RUN java -Dspring.aot.enabled=true -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefresh -jar application.jar; exit 0

FROM bellsoft/liberica-openjdk-debian:21.0.4-9-cds

ARG UID=1001
RUN adduser -u $UID ilivalidator

WORKDIR /work
RUN chown $UID:0 . && \
    chmod 0775 . && \
    ls -la
VOLUME ["/work"]

VOLUME /tmp
ARG DEPENDENCY=/workspace/app
COPY --from=optimizer ${DEPENDENCY}/application /app/application
COPY --from=optimizer ${DEPENDENCY}/application/application.jsa /app/application.jsa
WORKDIR /app/application

RUN chown -R $UID:0 . && \
    chmod -R 0775 . && \
    ls -la

USER $UID

ENTRYPOINT ["java",  "-XX:+UseG1GC", "-XX:MaxRAMPercentage=80.0", "-Dspring.aot.enabled=true", "-XX:SharedArchiveFile=application.jsa", "-jar", "application.jar", "--spring.profiles.active=docker"]


# FROM eclipse-temurin:21.0.3_9-jre-ubi9-minimal AS builder

# WORKDIR application
# ARG JAR_FILE=target/ilivalidator-web-service-server-*.jar
# COPY ${JAR_FILE} application.jar
# RUN java -Djarmode=layertools -jar application.jar extract


# FROM eclipse-temurin:21.0.3_9-jre-ubi9-minimal

# ARG UID=1001
# RUN adduser -u $UID ilivalidator

# WORKDIR /work
# RUN chown $UID:0 . && \
#     chmod 0775 . && \
#     ls -la
# VOLUME ["/work"]

# ENV HOME=/ilivalidator
# WORKDIR $HOME

# RUN chown $UID:0 . && \
#     chmod 0775 . && \
#     ls -la
# VOLUME ["/ilivalidator"]

# WORKDIR application

# RUN chown $UID:0 . && \
#     chmod 0775 . && \
#     ls -la

# COPY --chown=$UID:0 --chmod=0775 --from=builder application/dependencies/ ./
# COPY --chown=$UID:0 --chmod=0775 --from=builder application/spring-boot-loader/ ./
# COPY --chown=$UID:0 --chmod=0775 --from=builder application/snapshot-dependencies/ ./
# COPY --chown=$UID:0 --chmod=0775 --from=builder application/application/ ./

# USER $UID

# ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true
# ENTRYPOINT ["java", "-XX:+UseParallelGC", "-XX:MaxRAMPercentage=80.0", "org.springframework.boot.loader.launch.JarLauncher", "--spring.profiles.active=docker"]
