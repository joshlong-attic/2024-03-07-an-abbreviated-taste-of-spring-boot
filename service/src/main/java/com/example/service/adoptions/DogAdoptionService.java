package com.example.service.adoptions;

import com.example.service.grpc.AdoptionsGrpc;
import com.example.service.grpc.DogsResponse;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.checkerframework.checker.units.qual.C;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Controller
class DogAdoptionGraphqlController {


    private final DogAdoptionService dogAdoptionService;

    public DogAdoptionGraphqlController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @QueryMapping
    Collection<Dog> all() {
        return dogAdoptionService.all();
    }

    @MutationMapping
    boolean adopt(@Argument int dogId, @Argument String name) {
        dogAdoptionService.adopt(dogId, name);
        return true;
    }
}


@Service
class DogAdoptionGrpcService extends AdoptionsGrpc.AdoptionsImplBase {

    private final DogAdoptionService service;

    DogAdoptionGrpcService(DogAdoptionService service) {
        this.service = service;
    }

    @Override
    public void all(Empty request, StreamObserver<DogsResponse> responseObserver) {
        var all = service.all()
                .stream()
                .map(d -> com.example.service.grpc.Dog.newBuilder()
                        .setDescription(d.description())
                        .setName(d.name())
                        .setId(d.id())
                        .build())
                .toList();
        responseObserver.onNext(DogsResponse.newBuilder()
                .addAllDogs(all)
                .build());
        responseObserver.onCompleted();
    }
}


@Controller
@ResponseBody
class DogAdoptionHttpController {

    private final DogAdoptionService dogAdoptionService;

    DogAdoptionHttpController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @GetMapping("/assistant")
    String assistant(@RequestParam String question) {
        return dogAdoptionService.askAdoptionAssistant(question);
    }

    @GetMapping("/dogs")
    Collection<Dog> dogs() {
        return this.dogAdoptionService.all();
    }

    @PostMapping("/dogs/{dogId}/adoptions")
    void adopt(@PathVariable int dogId, @RequestParam String name) {
        dogAdoptionService.adopt(dogId, name);
    }
}

@Transactional
@Service
class DogAdoptionService {

    private final DogRepository dogRepository;
    private final ChatClient singularity;

    DogAdoptionService(DogRepository dogRepository, ChatClient singularity) {
        this.dogRepository = dogRepository;
        this.singularity = singularity;
    }

    String askAdoptionAssistant(String question) {
        return singularity
                .prompt()
                .user(question)
                .call()
                .content();

    }

    void adopt(int dogId, String name) {
        dogRepository.findById(dogId).ifPresent(dog -> {
            var newDog = this.dogRepository.save(new Dog(dog.id(), dog.name(), dog.description(), name));
            System.out.println("adopted [" + newDog + "]");
        });
    }

    Collection<Dog> all() {
        return this.dogRepository.findAll();
    }

}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record Dog(@Id int id, String name, String description, String owner) {
}

@Configuration
class ChatConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}