package ai.dataanalytic.databridge.config;

import ai.dataanalytic.databridge.service.ConnectionHolder;
import ai.dataanalytic.querybridge.config.DynamicDataSourceManager;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class DatabaseConfiguration extends DefaultBatchConfiguration {

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
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
    @JobScope
    @Qualifier("dataTransferStep")
    public Step dataTransferStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 @Value("#{jobParameters['jobId']}") String jobId,
                                 @Value("#{jobParameters['tableName']}") String tableName) {

        JdbcTemplate sourceJdbcTemplate = ConnectionHolder.getJdbcTemplate(jobId, "source");
        JdbcTemplate destinationJdbcTemplate = ConnectionHolder.getJdbcTemplate(jobId, "destination");

        if (sourceJdbcTemplate == null || destinationJdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplates not found for jobId: " + jobId);
        }

        return new StepBuilder("dataTransferStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(200, transactionManager)
                .reader(jdbcCursorItemReader(sourceJdbcTemplate, tableName))
                .writer(jdbcBatchItemWriter(destinationJdbcTemplate, tableName))
                .build();
    }

    public JdbcCursorItemReader<Map<String, Object>> jdbcCursorItemReader(JdbcTemplate jdbcTemplate, String tableName) {
        JdbcCursorItemReader<Map<String, Object>> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(jdbcTemplate.getDataSource());
        reader.setSql("SELECT * FROM " + tableName);
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

    private JdbcBatchItemWriter<Map<String, Object>> jdbcBatchItemWriter(JdbcTemplate jdbcTemplate, String tableName) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        String sql = generateInsertSql(jdbcTemplate, tableName);

        JdbcBatchItemWriter<Map<String, Object>> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new MapSqlParameterSourceProvider());
        writer.setSql(sql);
        writer.setJdbcTemplate(namedParameterJdbcTemplate);
        writer.afterPropertiesSet();
        return writer;
    }

    private String generateInsertSql(JdbcTemplate jdbcTemplate, String tableName) {
        // Retrieve columns from the destination table
        List<String> columns = getColumnNames(jdbcTemplate, tableName);
        String columnList = String.join(", ", columns);
        String parameterList = columns.stream().map(col -> ":" + col).collect(Collectors.joining(", "));
        return "INSERT INTO " + tableName + " (" + columnList + ") VALUES (" + parameterList + ")";
    }

    private List<String> getColumnNames(JdbcTemplate jdbcTemplate, String tableName) {
        String sql = "SELECT * FROM " + tableName + " WHERE 1=0";
        return jdbcTemplate.query(sql, rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            return columns;
        });
    }
}
