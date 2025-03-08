package com.example.service.adoptions;

import com.example.service.grpc.AdoptionsGrpc;
import com.example.service.grpc.DogsResponse;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;

@Service
class DogAdoptionsGrpcService
        extends AdoptionsGrpc.AdoptionsImplBase {

    private final DogAdoptionService service;

    DogAdoptionsGrpcService(DogAdoptionService service) {
        this.service = service;
    }

    @Override
    public void all(Empty request, StreamObserver<DogsResponse> responseObserver) {

        var all = this.service.all()
                .stream()
                .map(ogDog -> com.example.service.grpc.Dog.newBuilder()
                        .setId(ogDog.id())
                        .setName(ogDog.name())
                        .setDescription(ogDog.description())
                        .build())
                .toList();
        responseObserver.onNext(DogsResponse.newBuilder().addAllDogs(all).build());
        responseObserver.onCompleted();

    }
}

@Controller
@ResponseBody
class MeController {

    @GetMapping("/me")
    Map<String, String> me(Principal principal) {
        return Map.of("me", principal.getName());
    }
}


@Controller
class DogAdoptionGraphqlController {

    private final DogAdoptionService dogAdoptionService;

    DogAdoptionGraphqlController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @QueryMapping
    Collection<Dog> dogs() {
        return dogAdoptionService.all();
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
        System.out.println("dogs!");
        return this.dogAdoptionService.all();
    }

    @PostMapping("/dogs/{dogId}/adoptions")
    void adoptDog(@PathVariable int dogId, @RequestParam String owner) {
        this.dogAdoptionService.adopt(dogId, owner);
    }

    @GetMapping("/assist")
    String assist(@RequestParam String question) {
        return dogAdoptionService.assist(question);
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

    private final ChatClient singularity;
    private final DogRepository dogRepository;
    private final ApplicationEventPublisher eventPublisher;

    DogAdoptionService(ChatClient singularity, DogRepository dogRepository, ApplicationEventPublisher eventPublisher) {
        this.singularity = singularity;
        this.dogRepository = dogRepository;
        this.eventPublisher = eventPublisher;
    }

    void adopt(int dogId, String owner) {
        dogRepository.findById(dogId).ifPresent(dog -> {
            var updated = dogRepository.save(new Dog(dog.id(), dog.name(), owner, dog.description()));
            System.out.println("Dog adopted: " + updated);
            eventPublisher.publishEvent(new DogAdoptionEvent(dogId));
        });
    }

    Collection<Dog> all() {
        return dogRepository.findAll();
    }

    String assist(String question) {
        return this.singularity.prompt(question).call().content();
    }
}


interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

// look mom, no Lombok!
record Dog(@Id int id, String name, String owner, String description) {
}