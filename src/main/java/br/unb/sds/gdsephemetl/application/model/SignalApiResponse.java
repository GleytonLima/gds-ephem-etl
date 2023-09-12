package br.unb.sds.gdsephemetl.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class SignalApiResponse {
    @JsonProperty("_embedded")
    private Embedded embedded;

    @Data
    public static class Embedded {
        private List<SignalData> signals;
    }

    @Data
    public static class SignalData {
        private Long signalId;
        private JsonNode dados;
    }
}
