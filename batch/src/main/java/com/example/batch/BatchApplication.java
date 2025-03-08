package com.example.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@SpringBootApplication
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }


    @Bean
    FlatFileItemReader<Dog> dogFlatFileItemReader(
            @Value("file:${HOME}/Desktop/talk/dogs.csv") Resource resource) throws Exception {
        return new FlatFileItemReaderBuilder<Dog>()
                .name("dogFlatFileItemReader")
                .resource(resource)
                .linesToSkip(1)
                .fieldSetMapper(fieldSet -> new Dog(fieldSet.readInt("id"),
                        fieldSet.readString("owner"),
                        fieldSet.readString("dob"),
                        fieldSet.readString("name"),
                        fieldSet.readString("description")))
                .delimited()
                .names("id,name,description,dob,owner,gender,image".split(","))
                .build();
    }

    @Bean
    JdbcBatchItemWriter<Dog> dogJdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Dog>()
                .sql("""
                            insert into dog (id, name, description, owner) 
                            values (?, ?, ?, ?)
                        """)
                .dataSource(dataSource)
                .assertUpdates(true)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setInt(1, item.id());
                    ps.setString(2, item.name());
                    ps.setString(3, item.description());
                    ps.setString(4, item.owner());
                })
                .build();
    }

    @Bean
    Step csvToDbStep(JobRepository repository, PlatformTransactionManager transactionManager,
                     ItemReader<Dog> dogFlatFileItemReader, ItemWriter<Dog> dogItemWriter) {
        return new StepBuilder("csvToDbStep", repository)
                .<Dog, Dog>chunk(10, transactionManager)
                .reader(dogFlatFileItemReader)
                .writer(dogItemWriter)
                .build();
    }

    @Bean
    Job job(JobRepository repository, Step step) {
        return new JobBuilder("job", repository)
                .start(step)
                .incrementer(new RunIdIncrementer())
                .build();
    }
}

record Dog(int id, String owner, String dob, String name, String description) {
}