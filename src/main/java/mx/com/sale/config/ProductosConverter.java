package mx.com.sale.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import mx.com.sale.model.Producto;

import java.util.ArrayList;
import java.util.List;

@Converter
public class ProductosConverter implements AttributeConverter<List<Producto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(List<Producto> items) {
        if (items == null || items.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(items);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<Producto> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Producto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
