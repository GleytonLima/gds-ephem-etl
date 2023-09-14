package br.unb.sds.gdsephemetl.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class ModelApiResponse {
    @JsonProperty("_embedded")
    private Embedded embedded;

    @Data
    public static class Embedded {
        private List<ModelData> models;
    }

    @Data
    public static class ModelData {
        private JsonNode attributes;
    }
}
