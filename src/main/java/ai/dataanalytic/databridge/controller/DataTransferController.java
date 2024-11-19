package ai.dataanalytic.databridge.controller;

import ai.dataanalytic.databridge.dto.DataTransferRequest;
import ai.dataanalytic.databridge.service.ConnectionHolder;
import ai.dataanalytic.querybridge.service.DatabaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/data/bridge")
public class DataTransferController {

    private final JobLauncher jobLauncher;

    private final DatabaseService databaseService;


    private final Job dataTransferJob;

    public DataTransferController(JobLauncher jobLauncher, DatabaseService databaseService, Job dataTransferJob) {
        this.jobLauncher = jobLauncher;
        this.databaseService = databaseService;
        this.dataTransferJob = dataTransferJob;
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transferData(@RequestBody DataTransferRequest request, HttpSession session) {
        try {
            // Retrieve JdbcTemplates
            JdbcTemplate sourceJdbcTemplate = databaseService.getJdbcTemplateFromSession(session, request.getSourceConnectionId());
            JdbcTemplate destinationJdbcTemplate = databaseService.getJdbcTemplateFromSession(session, request.getDestinationConnectionId());

            if (sourceJdbcTemplate == null || destinationJdbcTemplate == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Database connections not found");
            }

            // Store JdbcTemplates in a shared context
            String jobId = UUID.randomUUID().toString();
            ConnectionHolder.storeJdbcTemplate(jobId, "source", sourceJdbcTemplate);
            ConnectionHolder.storeJdbcTemplate(jobId, "destination", destinationJdbcTemplate);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobId", jobId)
                    .addString("tableName", request.getTableName())
                    .toJobParameters();

            jobLauncher.run(dataTransferJob, jobParameters);

            return ResponseEntity.ok("Job started successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error starting job: " + e.getMessage());
        }
    }
}
