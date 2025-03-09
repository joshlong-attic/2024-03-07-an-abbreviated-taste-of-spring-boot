package com.example.integration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.MessageHeaders;

import java.util.function.BiConsumer;

@SpringBootApplication
public class IntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationApplication.class, args);
    }

    @Bean
    IntegrationFlow inboundIntegrationFlow(ConnectionFactory connectionFactory,
                                           @Value("file://${HOME}/Desktop/outbound") Resource resource) throws Exception {
        var file = resource.getFile();

        var amqp = Amqp.inboundAdapter(connectionFactory, "adoptions");
        return IntegrationFlow
                .from(amqp)
                .transform(new JsonToObjectTransformer(Dog.class))
                .handle((GenericHandler<Dog>) (payload, headers) -> {
                    System.out.println(payload);
                    headers.forEach((s, o) -> System.out.println(s + '=' + o));
                    return payload;
                })//
                .transform(new ObjectToJsonTransformer())
                .handle(Files.outboundAdapter(file).autoCreateDirectory(true))
                .get();
    }

}

record Dog(int dogId) {
}
