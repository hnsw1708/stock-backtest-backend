# ---------- Build stage (has Maven) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先拷贝 POM 并预拉依赖（加速后续构建）
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests dependency:go-offline

# 再拷贝源码并打包
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests clean package

# ---------- Run stage (slim JRE) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
