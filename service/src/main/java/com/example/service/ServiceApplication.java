package com.example.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }


}

/*
@Controller
@ResponseBody
class MeController {

    @GetMapping("/me")
    Map<String, String> token(Principal principal) {
        var map = new HashMap<>(Map.of("details", principal.getName()));
        if (principal instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            var token = jwtAuthenticationToken.getToken().getTokenValue();
            map.put("token", token);
        }
        return map;
    }
}*/
