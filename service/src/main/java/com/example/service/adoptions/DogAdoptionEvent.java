package com.example.service.adoptions;

import org.springframework.modulith.events.Externalized;

@Externalized("adoptionsOutboundMessageChannel")
public record DogAdoptionEvent(int dogId) {
}
