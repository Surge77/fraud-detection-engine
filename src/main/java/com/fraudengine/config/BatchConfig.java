package com.fraudengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudengine.batch.FraudReportTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Wires the nightly fraud-report batch job: a single Tasklet step that
 * aggregates one day's audit rows into a report row.
 */
@Configuration
public class BatchConfig {

    public static final String JOB_NAME = "fraudReportJob";

    @Bean
    public FraudReportTasklet fraudReportTasklet(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new FraudReportTasklet(jdbcTemplate, objectMapper);
    }

    @Bean
    public Step aggregateStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              FraudReportTasklet fraudReportTasklet) {
        return new StepBuilder("aggregateStep", jobRepository)
                .tasklet(fraudReportTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job fraudReportJob(JobRepository jobRepository, Step aggregateStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(aggregateStep)
                .build();
    }
}
