package com.Mazade.project.Common.Converters;

import com.Mazade.project.Common.Enums.Category;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter
public class CategoryMapConverter implements AttributeConverter<Map<Category, Integer>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<Category, Integer> attribute) {
        if (attribute == null) {
            return null;
        }

        // Convert enum keys to strings for JSON serialization
        Map<String, Integer> stringMap = new HashMap<>();
        for (Map.Entry<Category, Integer> entry : attribute.entrySet()) {
            stringMap.put(entry.getKey().name(), entry.getValue());
        }

        try {
            return objectMapper.writeValueAsString(stringMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting category map to JSON", e);
        }
    }

    @Override
    public Map<Category, Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        try {
            // First read as string map
            Map<String, Integer> stringMap = objectMapper.readValue(dbData,
                    new TypeReference<Map<String, Integer>>() {});

            // Convert string keys back to Category enum
            Map<Category, Integer> categoryMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : stringMap.entrySet()) {
                categoryMap.put(Category.valueOf(entry.getKey()), entry.getValue());
            }

            return categoryMap;
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to category map", e);
        }
    }
}