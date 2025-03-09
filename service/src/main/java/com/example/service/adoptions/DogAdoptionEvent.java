package com.example.service.adoptions;

import org.springframework.modulith.events.Externalized;

@Externalized(IntegrationConfiguration.ADOPTIONS_CHANNEL_NAME)
public record DogAdoptionEvent(int dogId) {
}
