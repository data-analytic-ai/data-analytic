package ai.dataanalytic.databridge.config;

import ai.dataanalytic.databridge.dto.DataTransferRequest;
import ai.dataanalytic.databridge.service.ConfigHolder;
import ai.dataanalytic.querybridge.config.DynamicDataSourceManager;
import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.*;
import org.springframework.batch.item.database.support.*;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Configuration
@EnableBatchProcessing
public class DatabaseConfiguration {

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new ResourcelessTransactionManager();
    }

    @Bean
    @Qualifier("dataTransferJob")
    public Job dataTransferJob(JobRepository jobRepository,
                               @Qualifier("dataTransferStep") Step dataTransferStep,
                               JobExecutionListener jobExecutionListener) {
        return new JobBuilder("dataTransferJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener)
                .start(dataTransferStep)
                .build();
    }

    @Bean
    @JobScope
    @Qualifier("dataTransferStep")
    public Step dataTransferStep(JobRepository jobRepository,
                                 @Value("#{jobParameters['jobId']}") String jobId,
                                 @Value("#{jobParameters['tableName']}") String tableName) {

        // Obtener la configuración del ConfigHolder
        DataTransferRequest config = ConfigHolder.getConfig(jobId);
        if (config == null) {
            throw new IllegalStateException("Config not found for jobId: " + jobId);
        }

        // Validar el nombre de la tabla
        if (!isValidIdentifier(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        // Crear DataSources utilizando DynamicDataSourceManager
        DataSource sourceDataSource = dynamicDataSourceManager.createDataSource(config.getSourceConnectionRequest());
        DataSource destinationDataSource = dynamicDataSourceManager.createDataSource(config.getDestinationConnectionRequest());

        // Almacenar los DataSources en el config
        config.setSourceDataSource(sourceDataSource);
        config.setDestinationDataSource(destinationDataSource);

        // Crear ItemReader y ItemWriter
        ItemReader<Map<String, Object>> reader = jdbcItemReader(sourceDataSource, tableName, config.getSourceConnectionRequest().getDatabaseType());
        ItemWriter<Map<String, Object>> writer = jdbcItemWriter(destinationDataSource, tableName);

        // Utilizar un transactionManager
        PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

        return new StepBuilder("dataTransferStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(100, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    public ItemReader<Map<String, Object>> jdbcItemReader(DataSource dataSource, String tableName, String databaseType) {
        JdbcPagingItemReader<Map<String, Object>> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setFetchSize(100);
        reader.setRowMapper(new ColumnMapRowMapper());

        // Configurar el proveedor de consultas según el tipo de base de datos
        PagingQueryProvider queryProvider = getPagingQueryProvider(databaseType, tableName);

        reader.setQueryProvider(queryProvider);

        try {
            reader.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing reader", e);
        }

        return reader;
    }

    private PagingQueryProvider getPagingQueryProvider(String databaseType, String tableName) {
        AbstractSqlPagingQueryProvider queryProvider;

        switch (databaseType.toLowerCase()) {
            case "mysql":
                queryProvider = new MySqlPagingQueryProvider();
                break;
            case "postgresql":
                queryProvider = new PostgresPagingQueryProvider();
                break;
            case "oracle":
                queryProvider = new OraclePagingQueryProvider();
                break;
            case "sqlserver":
                queryProvider = new SqlServerPagingQueryProvider();
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }

        queryProvider.setSelectClause("SELECT *");
        queryProvider.setFromClause("FROM " + tableName);
        queryProvider.setSortKeys(Collections.singletonMap("id", Order.ASCENDING));

        return queryProvider;
    }

    public ItemWriter<Map<String, Object>> jdbcItemWriter(DataSource dataSource, String tableName) {
        JdbcBatchItemWriter<Map<String, Object>> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setItemSqlParameterSourceProvider(new MapSqlParameterSourceProvider());

        String sql = generateInsertSql(dataSource, tableName);
        writer.setSql(sql);

        try {
            writer.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing writer", e);
        }

        return writer;
    }

    private String generateInsertSql(DataSource dataSource, String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> columns = getColumnNames(jdbcTemplate, tableName);

        String columnList = columns.stream().map(col -> "`" + col + "`").collect(Collectors.joining(", "));
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

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                // No es necesario realizar acciones antes del trabajo
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                String jobId = jobExecution.getJobParameters().getString("jobId");
                DataTransferRequest config = ConfigHolder.getConfig(jobId);

                if (config != null) {
                    // Cerrar DataSources
                    dynamicDataSourceManager.closeDataSource(config.getSourceDataSource());
                    dynamicDataSourceManager.closeDataSource(config.getDestinationDataSource());

                    // Remover la configuración del ConfigHolder
                    ConfigHolder.removeConfig(jobId);
                }
            }
        };
    }

    private boolean isValidIdentifier(String identifier) {
        return identifier != null && identifier.matches("^[a-zA-Z0-9_]+$");
    }
}
