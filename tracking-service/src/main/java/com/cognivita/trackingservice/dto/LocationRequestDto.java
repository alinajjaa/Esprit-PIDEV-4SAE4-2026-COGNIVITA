package com.cognivita.trackingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationRequestDto {

    @JsonProperty("_type")
    private String _type;
    private Double lat;
    private Double lon;
    private Long tst;
    private Double acc;
    private Double vel;
    private String tid;
    private List<String> motionactivities;
}
