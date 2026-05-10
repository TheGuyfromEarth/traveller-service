# ---------- Layer extraction stage ----------
# Use a tiny intermediate stage to split the fat JAR into Spring Boot's layers.
# This dramatically improves Docker cache reuse — dependency changes are rare,
# code changes are frequent, so we want them in separate layers.
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /app
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy layers in order of change frequency (least -> most).
# Each COPY becomes its own image layer, so Docker only rebuilds what changed.
COPY --from=extractor /app/extracted/dependencies/ ./
COPY --from=extractor /app/extracted/spring-boot-loader/ ./
COPY --from=extractor /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/extracted/application/ ./

# Cloud Run injects PORT; Spring Boot must bind to it
EXPOSE 8080

# JVM tuning critical for 512 MiB Cloud Run containers:
#   MaxRAMPercentage=75 -> heap ~ 384 MiB (leaves room for metaspace, threads, native)
#   UseSerialGC         -> smallest GC footprint, ideal for single-CPU containers
#   TieredStopAtLevel=1 -> skip tier-2 JIT compilation, faster startup
#   lazy-initialization -> defer bean creation until first use
#   exec via sh so $PORT expands and signals (SIGTERM) propagate correctly
ENTRYPOINT ["sh", "-c", "exec java \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseSerialGC \
  -XX:TieredStopAtLevel=1 \
  -Dspring.main.lazy-initialization=true \
  -Dspring.jmx.enabled=false \
  -Dserver.port=${PORT:-8080} \
  -Djava.security.egd=file:/dev/./urandom \
  org.springframework.boot.loader.launch.JarLauncher"]