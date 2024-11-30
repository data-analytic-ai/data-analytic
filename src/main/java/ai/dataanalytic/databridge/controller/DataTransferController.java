package ai.dataanalytic.databridge.controller;

import ai.dataanalytic.databridge.dto.DataTransferRequest;
import ai.dataanalytic.databridge.service.ConfigHolder;
import ai.dataanalytic.databridge.service.ConnectionHolder;
import ai.dataanalytic.querybridge.config.DynamicDataSourceManager;
import ai.dataanalytic.querybridge.service.DatabaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;@RestController
@RequestMapping("/api/data-transfer")
public class DataTransferController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("dataTransferJob")
    private Job dataTransferJob;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @PostMapping("/start")
    public ResponseEntity<String> startDataTransfer(@RequestBody DataTransferRequest config) {
        try {
            // Generar un jobId único
            String jobId = UUID.randomUUID().toString();

            // Almacenar la configuración en un mapa estático para acceder desde el Job
            ConfigHolder.addConfig(jobId, config);

            // Crear JobParameters
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobId", jobId)
                    .addString("tableName", config.getTableName())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            // Lanzar el trabajo
            JobExecution jobExecution = jobLauncher.run(dataTransferJob, jobParameters);

            return ResponseEntity.ok("Job started with ID: " + jobExecution.getId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error starting job: " + e.getMessage());
        }
    }
}