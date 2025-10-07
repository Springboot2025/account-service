package com.legalpro.accountservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/autocomplete")
    public ResponseEntity<String> autocomplete(@RequestParam String input) {
        String url = "https://places.googleapis.com/v1/places:autocomplete";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask", "suggestions.placePrediction.placeId,suggestions.placePrediction.text.text");

        Map<String, Object> requestBody = Map.of("input", input);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/details")
    public ResponseEntity<String> getPlaceDetails(@RequestParam String placeId) {
        String url = "https://places.googleapis.com/v1/places/" + placeId
                + "?fields=formattedAddress,addressComponents,location";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask", "formattedAddress,addressComponents,location");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return ResponseEntity.ok(response.getBody());
    }
}
