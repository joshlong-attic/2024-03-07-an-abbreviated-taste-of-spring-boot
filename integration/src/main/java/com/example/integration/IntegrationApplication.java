package com.example.integration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.messaging.MessageHeaders;

@SpringBootApplication
public class IntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationApplication.class, args);
    }

    @Bean
    IntegrationFlow integrationFlow(ConnectionFactory connectionFactory,
                                    @Value("file://${HOME}/Desktop/outbound") Resource resource) throws Exception {
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, "adoptions"))
                .transform(new JsonToObjectTransformer(Dog.class))
                .handle((GenericHandler<Dog>) (payload, headers) -> {
                    System.out.println("Payload: " + payload);
                    return payload;
                })
                .transform(new ObjectToJsonTransformer())
                .handle(Files.outboundAdapter(resource.getFile()))
                .get();

    }
}

record Dog(int dogId) {
}