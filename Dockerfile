# ==============================================================================
# Spring Boot Dockerfile (Java 21)
# ==============================================================================

# 1. Base Image: Java 21 JRE (실행 전용 경량화 이미지)
FROM eclipse-temurin:21-jre-alpine

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. JAR 파일 복사
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 4. 오라클 전자지갑(Wallet) 파일 복사 (매우 중요!)
# EC2에 업로드한 wallet 폴더를 도커 내부의 /app/wallet 경로로 복사합니다.
COPY wallet /app/wallet

# 5. 시간대(Timezone) 설정 (한국 시간)
RUN apk --no-cache add tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 6. 실행 포트 노출
EXPOSE 8080

# 7. 어플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "-Duser.timezone=Asia/Seoul", "app.jar"]
