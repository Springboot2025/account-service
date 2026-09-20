package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.AddressDetailsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        try {
            return ResponseEntity.ok(typeAheadSearch(input));
        } catch (Exception ex) {
            // e.g. the key isn't enabled for the autocomplete endpoint --
            // still return something useful rather than an empty dropdown.
            return ResponseEntity.ok(textSearchStartingWith(input));
        }
    }

   
    @SuppressWarnings("unchecked")
    private Map<String, Object> typeAheadSearch(String input) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("input", input);
        body.put("includedRegionCodes", List.of("au"));
        body.put("regionCode", "AU");
        body.put("languageCode", "en-AU");

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://places.googleapis.com/v1/places:autocomplete",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        List<Map<String, Object>> places = new ArrayList<>();
        Object suggestions = response.getBody() == null ? null : response.getBody().get("suggestions");
        if (suggestions instanceof List<?> suggestionList) {
            for (Object suggestion : suggestionList) {
                if (!(suggestion instanceof Map<?, ?> suggestionMap)
                        || !(suggestionMap.get("placePrediction") instanceof Map<?, ?> prediction)) {
                    continue; // a "query prediction" (a search phrase), not a place
                }

                Object placeId = prediction.get("placeId");
                String fullText = nestedText(prediction.get("text"));
                if (placeId == null || fullText == null) continue;

                Map<?, ?> structured = prediction.get("structuredFormat") instanceof Map<?, ?> m ? m : Map.of();
                String mainText = nestedText(structured.get("mainText"));

                Map<String, Object> place = new LinkedHashMap<>();
                place.put("id", placeId);
                place.put("displayName", Map.of("text", mainText != null ? mainText : fullText));
                place.put("formattedAddress", fullText.replaceFirst(",\\s*Australia$", ""));
                places.add(place);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("places", places);
        return result;
    }

    private static String nestedText(Object holder) {
        return holder instanceof Map<?, ?> map && map.get("text") instanceof String text ? text : null;
    }

    /**
     * Fallback if the type-ahead call fails: the old Text Search, kept only to
     * results that start with what was typed (in the place's name or its
     * address) so indirect matches still don't leak through.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> textSearchStartingWith(String input) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask",
                "places.id,places.displayName.text,places.formattedAddress");

        Map<String, Object> body = new HashMap<>();
        body.put("textQuery", "address in Australia " + input);
        body.put("regionCode", "AU");
        body.put("languageCode", "en-AU");
        Map<String, Object> low = Map.of("latitude", -44.0, "longitude", 112.0);
        Map<String, Object> high = Map.of("latitude", -10.0, "longitude", 154.0);
        body.put("locationRestriction", Map.of("rectangle", Map.of("low", low, "high", high)));

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://places.googleapis.com/v1/places:searchText",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        return startingWith(response.getBody(), input);
    }

    /** Keeps places whose name or address starts with the typed text. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> startingWith(Map<String, Object> body, String input) {
        if (body == null || !(body.get("places") instanceof List<?> places)) {
            return body;
        }
        String typed = input.trim().toLowerCase();

        List<Object> matching = places.stream()
                .filter(place -> {
                    if (!(place instanceof Map<?, ?> map)) return false;
                    String name = nestedText(map.get("displayName"));
                    Object address = map.get("formattedAddress");
                    return (name != null && name.toLowerCase().startsWith(typed))
                            || (address instanceof String a && a.toLowerCase().startsWith(typed));
                })
                .map(place -> (Object) place)
                .toList();

        Map<String, Object> filtered = new HashMap<>(body);
        filtered.put("places", matching);
        return filtered;
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
