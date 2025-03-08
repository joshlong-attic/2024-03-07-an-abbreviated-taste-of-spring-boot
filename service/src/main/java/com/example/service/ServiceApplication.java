package com.example.service;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.*;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

    @Bean
    IntegrationFlow outboundFlow(MessageChannel outboundMessageChannel,
                                 AmqpTemplate amqpTemplate) {
        return flow -> flow
                .handle(Amqp
                        .outboundAdapter(amqpTemplate)
                        .exchangeName("messages")
                );
    }

    @Bean
    MessageChannelSpec<DirectChannelSpec, DirectChannel> outboundMessageChannel() {
        return MessageChannels.direct();
    }

}
