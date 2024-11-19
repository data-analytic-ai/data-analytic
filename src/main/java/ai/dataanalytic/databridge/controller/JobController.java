package ai.dataanalytic.databridge.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class JobController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("dataTransferJob")
    private Job job;  // This is the job you defined in the config

    @PostMapping("/batch/run")
    public ResponseEntity<String> runJob() {
        try {
            // Generate a unique job name with the current timestamp
            String jobName = "Job_" + System.currentTimeMillis();

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobName", jobName)  // Passing the dynamic job name
                    .addDate("runDate", new Date())  // Add other job parameters if needed
                    .toJobParameters();

            // Launch the job with the provided parameters
            jobLauncher.run(job, jobParameters);

            return ResponseEntity.ok("Job triggered successfully with name: " + jobName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to trigger job.");
        }
    }
}
