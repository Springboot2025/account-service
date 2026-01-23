package com.legalpro.accountservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.LanguageDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LanguageService {

    private final ObjectMapper objectMapper;
    private List<LanguageDto> languages;

    @PostConstruct
    public void loadLanguages() {
        try {
            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("languages.json");

            languages = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<LanguageDto>>() {}
            );

        } catch (Exception e) {
            e.printStackTrace();
            languages = Collections.emptyList();
        }
    }

    public List<LanguageDto> getAllLanguages() {
        return languages;
    }
}
