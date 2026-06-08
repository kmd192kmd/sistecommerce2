package com.example.java.delivery.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job deliveryUpdateJob;

    /**
     * 외부 스케줄러(Jenkins 등)에서 호출하여 배치를 강제로 실행하는 API 엔드포인트입니다.
     */
    @PostMapping("/run-delivery")
    public ResponseEntity<String> runDeliveryBatch() {
        try {
            log.info("Jenkins triggered Delivery Batch Job...");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            jobLauncher.run(deliveryUpdateJob, jobParameters);
            
            log.info("Jenkins triggered Delivery Batch Job completed.");
            return ResponseEntity.ok("Batch Job Executed Successfully");
        } catch (Exception e) {
            log.error("Failed to execute Batch Job from Jenkins", e);
            return ResponseEntity.internalServerError().body("Batch Job Failed: " + e.getMessage());
        }
    }
}
