package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.AddressDetailsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/autocomplete")
    public ResponseEntity<Map> searchAddress(@RequestParam String input) {

        String url = "https://places.googleapis.com/v1/places:searchText";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);

        headers.set("X-Goog-FieldMask",
                "places.id," +
                        "places.displayName.text," +
                        "places.formattedAddress," +
                        "places.location," +
                        "places.addressComponents"
        );

        Map<String, Object> body = new HashMap<>();

        // ⭐ Forces AU-only + address-only
        body.put("textQuery", "address in Australia " + input);

        body.put("regionCode", "AU");
        body.put("languageCode", "en-AU");

        // ⭐ Strict bounding box for Australia
        Map<String, Object> low = Map.of("latitude", -44.0, "longitude", 112.0);
        Map<String, Object> high = Map.of("latitude", -10.0, "longitude", 154.0);
        body.put("locationRestriction", Map.of("rectangle", Map.of("low", low, "high", high)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/details")
    public ResponseEntity<AddressDetailsDto> getPlaceDetails(@RequestParam String placeId) {

        String url = "https://places.googleapis.com/v1/places/" + placeId
                + "?fields=formattedAddress,addressComponents";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask", "formattedAddress,addressComponents");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map body = response.getBody();
        AddressDetailsDto dto = parseAddressDetails(body);

        return ResponseEntity.ok(dto);
    }

    @SuppressWarnings("unchecked")
    private AddressDetailsDto parseAddressDetails(Map body) {

        AddressDetailsDto dto = new AddressDetailsDto();

        dto.setFormattedAddress((String) body.get("formattedAddress"));

        var components = (java.util.List<Map<String, Object>>) body.get("addressComponents");

        String streetNumber = null;
        String route = null;

        if (components != null) {
            for (Map<String, Object> comp : components) {
                String longText = (String) comp.get("longText");
                String shortText = (String) comp.get("shortText");
                var types = (java.util.List<String>) comp.get("types");

                if (types.contains("street_number")) {
                    streetNumber = longText;   // e.g., 70
                }
                if (types.contains("route")) {
                    route = longText;         // e.g., Southbank Boulevard
                }
                if (types.contains("locality") || types.contains("sublocality") || types.contains("postal_town")) {
                    dto.setCity(longText);
                }
                if (types.contains("administrative_area_level_1")) {
                    dto.setState(shortText);
                }
                if (types.contains("postal_code")) {
                    dto.setPostcode(longText);
                }
                if (types.contains("country")) {
                    dto.setCountry(longText);
                }
            }
        }

        // Build street address correctly
        if (streetNumber != null && route != null) {
            dto.setStreetAddress(streetNumber + " " + route);
        } else if (route != null) {
            dto.setStreetAddress(route);
        }

        dto.setUnit(null); // Only set if building has apartment/suite — Google rarely gives this

        return dto;
    }
}
