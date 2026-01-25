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
    public ResponseEntity<Map<String, Object>> autocomplete(@RequestParam String input) {

        String url = "https://places.googleapis.com/v1/places:autocomplete";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);

        headers.set("X-Goog-FieldMask",
                "suggestions.placePrediction.placeId," +
                        "suggestions.placePrediction.structuredFormat.mainText," +
                        "suggestions.placePrediction.structuredFormat.secondaryText," +
                        "suggestions.placePrediction.placeTypes," +
                        "suggestions.placePrediction.location"
        );

        Map<String, Object> body = new HashMap<>();
        body.put("input", input);
        body.put("regionCode", "AU");
        body.put("languageCode", "en-AU");

        // AU bounding box
        Map<String, Object> low = Map.of("latitude", -44.0, "longitude", 112.0);
        Map<String, Object> high = Map.of("latitude", -10.0, "longitude", 154.0);
        Map<String, Object> rect = Map.of("low", low, "high", high);
        body.put("locationRestriction", Map.of("rectangle", rect));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        // ⭐ Filter results manually (address-only + AU-only)
        Map<String, Object> filtered = filterAutocompleteResults(response.getBody());

        return ResponseEntity.ok(filtered);
    }


    @GetMapping("/details")
    public ResponseEntity<AddressDetailsDto> getPlaceDetails(@RequestParam String placeId) {

        String url = "https://places.googleapis.com/v1/places/" + placeId +
                "?fields=formattedAddress,addressComponents";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask", "formattedAddress,addressComponents");

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        AddressDetailsDto dto = parseAddress(response.getBody());

        return ResponseEntity.ok(dto);
    }

    @SuppressWarnings("unchecked")
    private AddressDetailsDto parseAddress(Map body) {

        AddressDetailsDto dto = new AddressDetailsDto();

        dto.setFormattedAddress((String) body.get("formattedAddress"));

        List<Map<String, Object>> comps =
                (List<Map<String, Object>>) body.get("addressComponents");

        String streetNumber = null;
        String route = null;
        String unit = null;

        if (comps != null) {
            for (Map<String, Object> comp : comps) {

                String longText = (String) comp.get("longText");
                String shortText = (String) comp.get("shortText");
                List<String> types = (List<String>) comp.get("types");

                if (types.contains("street_number")) {
                    streetNumber = longText;
                }
                if (types.contains("route")) {
                    route = longText;
                }
                if (types.contains("subpremise")) {
                    unit = longText;   // Apartment / Level / Unit
                }
                if (types.contains("locality") || types.contains("postal_town") || types.contains("sublocality")) {
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

        // Build final street address (matches UI)
        if (streetNumber != null && route != null) {
            dto.setStreetAddress(streetNumber + " " + route);
        } else {
            dto.setStreetAddress(route);
        }

        dto.setUnit(unit);

        return dto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterAutocompleteResults(Map<String, Object> original) {

        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) original.get("suggestions");

        if (suggestions == null) return original;

        List<Map<String, Object>> filteredList = suggestions.stream()
                .filter(s -> {
                    Map<String, Object> pred = (Map<String, Object>) s.get("placePrediction");

                    List<String> types = (List<String>) pred.get("placeTypes");
                    if (types == null) return false;

                    // ⭐ Accept ONLY these types (AU-address match UI)
                    return types.contains("street_address")
                            || types.contains("route")
                            || types.contains("premise")      // building with address
                            || types.contains("subpremise");  // apartment/unit
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", filteredList);

        return result;
    }
}
