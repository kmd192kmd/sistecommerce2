/*
 * =============================================================================
 * 🚨 JENKINS PIPELINE INACTIVE (DISABLED)
 * =============================================================================
 * [비활성화 사유]
 * - 현재 인프라 환경: AWS EC2 Small (RAM 2GB) 및 Elasticsearch 구동 중.
 * - Jenkins 구동 시 발생하는 심각한 OOM (Out Of Memory) 및 CPU/디스크 병목(I/O Wait)을 
 *   방지하기 위해 Jenkins 파이프라인 및 배송 배치 트리거 스케줄러를 비활성화합니다.
 * - 추후 빌드 및 배포 자동화는 GitHub Actions(클라우드 런너 빌드 방식)로 대체할 것을 권장합니다.
 * =============================================================================

pipeline {
    agent any

    // 젠킨스 파이프라인의 매개변수 설정 (수동 실행 시 사용 가능)
    parameters {
        booleanParam(name: 'FORCE_RUN', defaultValue: false, description: '수동으로 배치를 즉시 실행할까요?')
    }

    // 트리거(스케줄러) 설정: 매시간 정각마다 실행
    triggers {
        cron('0 * * * *')
    }

    stages {
        stage('Trigger Spring Batch') {aaaaa
            steps {
                script {
                    echo "================================================="
                    echo "🚀 Starting Delivery Update Batch Job Execution..."
                    echo "================================================="

                    // 호스트 PC(스프링 부트 서버)의 API를 호출하여 배치를 실행합니다.
                    try {
                        def response = sh(script: "curl -s -X POST http://host.docker.internal:8080/api/batch/run-delivery", returnStdout: true).trim()
                        echo "API Response: ${response}"
                        
                        if (response.contains("Failed")) {
                            error("Batch failed with response: ${response}")
                        }
                    } catch (Exception e) {
                        echo "❌ Error triggering batch: ${e.getMessage()}"
                        error("Failed to reach Spring Boot Batch API. Is the server running on port 8080?")
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Batch Job finished successfully."
        }
        failure {
            echo "🚨 Batch Job failed. Check Spring Boot server logs."
        }
    }
}
*/
