package com.example.service.vet;

import com.example.service.adoptions.DogAdoptionEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
class Dogtor {

    @ApplicationModuleListener
    void checkup(DogAdoptionEvent dogId) throws InterruptedException {
        Thread.sleep(5000);
        System.out.println("checking up on " + dogId);
    }
}
