package com.example.service.adoptions;

import com.example.service.adoptions.grpc.AdoptionsGrpc;
import com.example.service.adoptions.grpc.DogsResponse;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Service
class DogAdoptionsGrpcService extends AdoptionsGrpc.AdoptionsImplBase {

    private final DogAdoptionService dogAdoptionService;

    DogAdoptionsGrpcService(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @Override
    public void all(Empty request, StreamObserver<DogsResponse> responseObserver) {

        var all = dogAdoptionService.dogs()
                .stream()
                .map(ogDog -> com.example.service.adoptions.grpc.Dog.newBuilder()
                        .setId(ogDog.id())
                        .setName(ogDog.name())
                        .setDescription(ogDog.description())
                        .build())
                .toList();

        var reply = DogsResponse.newBuilder().addAllDogs(all).build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}


@Controller
class DogAdoptionsGraphqlController {

    private final DogAdoptionService dogAdoptionService;

    DogAdoptionsGraphqlController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @QueryMapping
    Collection<Dog> dogs() {
        return dogAdoptionService.dogs();
    }
}


@Configuration
class IntegrationConfiguration {

    static final String ADOPTIONS_CHANNEL_NAME = "outboundAdoptionsMessageChannel";

    static final String ADOPTIONS_NAME = "adoptions";

    @Bean
    Queue adoptionsQueue() {
        return QueueBuilder.durable(ADOPTIONS_NAME).build();
    }

    @Bean
    Exchange adoptionsExchange() {
        return ExchangeBuilder.directExchange(ADOPTIONS_NAME).build();
    }

    @Bean
    Binding adoptionsBinding(Queue adoptionsQueue, Exchange adoptionsExchange) {
        return BindingBuilder
                .bind(adoptionsQueue)
                .to(adoptionsExchange)
                .with(ADOPTIONS_NAME)
                .noargs();
    }

    @Bean(ADOPTIONS_CHANNEL_NAME)
    MessageChannelSpec<DirectChannelSpec, DirectChannel> outboundAdoptionsMessageChannel() {
        return MessageChannels.direct();
    }

    @Bean
    IntegrationFlow outboundAdoptionsIntegrationFlow(
            AmqpTemplate amqpTemplate,
            @Qualifier(ADOPTIONS_CHANNEL_NAME) MessageChannel messageChannel) {
        return IntegrationFlow
                .from(messageChannel)
                .handle(Amqp.outboundAdapter(amqpTemplate)
                        .routingKey(ADOPTIONS_NAME)
                        .exchangeName(ADOPTIONS_NAME)
                )
                .get();
    }

}

@Controller
@ResponseBody
class DogAdoptionHttpController {

    private final DogAdoptionService dogAdoptionService;

    DogAdoptionHttpController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @GetMapping("/dogs")
    Collection<Dog> dogs() {
        return dogAdoptionService.dogs();
    }

    @GetMapping("/assistant")
    String assistant(@RequestParam String question) {
        return dogAdoptionService.assistant(question);
    }

    @PostMapping("/dogs/{dogId}/adoptions")
    void adopt(@PathVariable int dogId, @RequestParam String owner) {
        this.dogAdoptionService.adopt(dogId, owner);
    }
}

@Configuration
class AiConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}

@Service
@Transactional
class DogAdoptionService {

    private final DogRepository dogRepository;
    private final ChatClient singularity;
    private final ApplicationEventPublisher applicationEventPublisher;

    DogAdoptionService(DogRepository dogRepository, ChatClient singularity, ApplicationEventPublisher applicationEventPublisher) {
        this.dogRepository = dogRepository;
        this.singularity = singularity;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    void adopt(int dogId, String owner) {
        dogRepository.findById(dogId).ifPresent(dog -> {
            var updated = dogRepository.save(new Dog(dog.id(), dog.name(), owner, dog.description()));
            applicationEventPublisher.publishEvent(new DogAdoptionEvent(dogId));
            System.out.println("updated [" + updated + "]");
        });
    }

    Collection<Dog> dogs() {
        return dogRepository.findAll();
    }

    String assistant(String question) {
        return this.singularity.prompt(question).call().content();
    }
}

record Dog(@Id int id, String name, String owner, String description) {
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}




