package ai.dataanalytic.databridge.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfiguration extends DefaultBatchConfiguration {

    @Bean
    @Primary
    @Qualifier("dataSource")
    @ConfigurationProperties(prefix = "db.job.repo")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();  // Remove HikariDataSource specification
    }


    @Bean
    @Qualifier("sourceDataSource")
    @ConfigurationProperties(prefix = "db.source")
    public DataSource sourceDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Qualifier("destinationDataSource")
    @ConfigurationProperties(prefix = "db.destination")
    public DataSource destinationDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        JdbcTransactionManager transactionManager = new JdbcTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }

    @Bean
    @Qualifier("dataTransferJob")
    public Job dataTransferJob(JobRepository jobRepository, @Qualifier("dataTransferStep") Step dataTransferStep) {
        return new JobBuilder("dataTransferJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dataTransferStep)
                .build();
    }

    @Bean
    @Qualifier("dataTransferStep")
    public Step dataTransferStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("sourceDataSource") DataSource sourceDataSource, @Qualifier("destinationDataSource") DataSource destinationDataSource) {
        return new StepBuilder("dataTransferStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(200, transactionManager)
                .reader(jdbcCursorItemReader(sourceDataSource))
                .writer(jdbcBatchItemWriter(destinationDataSource))
                .build();
    }


    @Bean
    public JdbcCursorItemReader<Map<String, Object>> jdbcCursorItemReader(DataSource sourceDataSource) {
        JdbcCursorItemReader<Map<String, Object>> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(sourceDataSource);
        reader.setSql("SELECT * FROM student"); // You can modify the SQL query as needed
        reader.setRowMapper((rs, rowNum) -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            return row;
        });
        return reader;
    }

    @Bean
    public JdbcBatchItemWriter<Map<String, Object>> jdbcBatchItemWriter(DataSource destinationDataSource) {
        JdbcBatchItemWriter<Map<String, Object>> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new MapSqlParameterSourceProvider());

        // You will need to construct the SQL dynamically to match the destination table schema
        writer.setSql("INSERT INTO student (id, first_name, last_name, email, dept_id, is_active) "
                + "VALUES (:id, :first_name, :last_name, :email, :dept_id, :is_active)");
        writer.setDataSource(destinationDataSource);
        return writer;
    }


}
