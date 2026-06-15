/* =========================================================================
   [ 포트폴리오 비교용: 단일 서버 환경의 Spring @Scheduled 버전을 주석 처리함 ]
   -------------------------------------------------------------------------
   기존에는 이 클래스가 매시간 정각에 동작하여 서버 내부에서 배치를 실행했습니다.
   하지만 대규모 트래픽 환경(스케일 아웃)에서는 서버가 여러 대가 되므로,
   서버 내부 스케줄러를 쓰면 배치가 여러 번 중복 실행되는 치명적인 문제가 발생합니다.
   
   따라서 이 내부 스케줄러를 주석 처리하고, 외부의 전문 스케줄러 서버인 
   Jenkins(젠킨스)가 REST API(BatchController)를 호출하여 통제하도록 
   MSA(마이크로서비스) 지향적으로 아키텍처를 고도화했습니다.
========================================================================= */

package com.example.java.delivery.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job deliveryUpdateJob;

    @Scheduled(cron = "0 0 * * * *")
    public void runDeliveryJobHourly() {
        try {
            log.info("Starting hourly Delivery Batch Job...");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            jobLauncher.run(deliveryUpdateJob, jobParameters);
            
            log.info("Hourly Delivery Batch Job completed successfully.");
        } catch (Exception e) {
            log.error("Failed to execute Delivery Batch Job", e);
        }
    }
}
