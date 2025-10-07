package com.legalpro.accountservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/autocomplete")
    public ResponseEntity<String> autocomplete(@RequestParam String input) {
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json"
                + "?input=" + input
                + "&types=address"
                + "&key=" + googleApiKey;

        String response = restTemplate.getForObject(url, String.class);
        return ResponseEntity.ok(response);
    }
}
