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
        stage('Trigger Spring Batch') {
            steps {
                script {
                    echo "================================================="
                    echo "🚀 Starting Delivery Update Batch Job Execution..."
                    echo "================================================="

                    // 호스트 PC(스프링 부트 서버)의 API를 호출하여 배치를 실행합니다.
                    // Jenkins Docker 컨테이너에서 로컬 윈도우 PC로 접근하기 위해 host.docker.internal 사용
                    try {
                        def response = sh(script: "curl -s -X POST http://host.docker.internal:8080/api/batch/run-delivery", returnStdout: true).trim()
                        echo "API Response: ${response}"
                        
                        if (response.contains("Failed")) {
                            error("Batch failed with response: ${response}")
                        }
                    } catch (Exception e) {
                        echo "❌ Error triggering batch: ${e.getMessage()}"
                        // 스프링 부트 서버가 꺼져있을 경우 등 예외 처리
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
