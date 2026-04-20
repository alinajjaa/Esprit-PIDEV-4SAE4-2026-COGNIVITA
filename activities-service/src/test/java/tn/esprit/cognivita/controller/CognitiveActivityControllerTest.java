package tn.esprit.cognivita.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.cognivita.entity.CognitiveActivity;
import tn.esprit.cognivita.service.CognitiveActivityService;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CognitiveActivityController.class)
class CognitiveActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CognitiveActivityService service;

    @Autowired
    private ObjectMapper mapper;

    private CognitiveActivity sample;

    @BeforeEach
    void setUp() {
        sample = new CognitiveActivity();
        sample.setId(1L);
        sample.setTitle("Sample");
        sample.setType("MEMORY");
        sample.setIsActive(true);
        sample.setWords(Arrays.asList("a","b","c"));
    }

    @Test
    @DisplayName("GET /activities should return list")
    void getAllActivities_returnsList() throws Exception {
        when(service.getAllActivities()).thenReturn(Collections.singletonList(sample));

        mockMvc.perform(get("/activities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Sample"));

        verify(service).getAllActivities();
    }

    @Test
    @DisplayName("GET /activities/{id} should return activity")
    void getActivityById_returnsActivity() throws Exception {
        when(service.getActivityById(1L)).thenReturn(sample);

        mockMvc.perform(get("/activities/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Sample"));

        verify(service).getActivityById(1L);
    }

    @Test
    @DisplayName("POST /activities should create activity")
    void createActivity_creates() throws Exception {
        when(service.createActivity(any(CognitiveActivity.class))).thenAnswer(i -> {
            CognitiveActivity a = i.getArgument(0);
            a.setId(5L);
            return a;
        });

        mockMvc.perform(post("/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(sample)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));

        verify(service).createActivity(any(CognitiveActivity.class));
    }

    @Test
    @DisplayName("PUT /activities/{id} should update when exists")
    void updateActivity_updates() throws Exception {
        CognitiveActivity updated = new CognitiveActivity();
        updated.setId(1L);
        updated.setTitle("Updated");

        when(service.updateActivity(eq(1L), any(CognitiveActivity.class))).thenReturn(updated);

        mockMvc.perform(put("/activities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        verify(service).updateActivity(eq(1L), any(CognitiveActivity.class));
    }

    @Test
    @DisplayName("DELETE /activities/{id} should delete when exists")
    void deleteActivity_deletes() throws Exception {
        doNothing().when(service).deleteActivity(1L);

        mockMvc.perform(delete("/activities/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteActivity(1L);
    }

    @Test
    @DisplayName("GET /activities/type/{type} should filter by type")
    void getByType_filters() throws Exception {
        when(service.getActivitiesByType("MEMORY")).thenReturn(Collections.singletonList(sample));

        mockMvc.perform(get("/activities/type/MEMORY").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MEMORY"));

        verify(service).getActivitiesByType("MEMORY");
    }

    @Test
    @DisplayName("GET /activities/difficulty/{difficulty} should filter by difficulty")
    void getByDifficulty_filters() throws Exception {
        sample.setDifficulty("MEDIUM");
        when(service.getActivitiesByDifficulty("MEDIUM")).thenReturn(Collections.singletonList(sample));

        mockMvc.perform(get("/activities/difficulty/MEDIUM").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value("MEDIUM"));

        verify(service).getActivitiesByDifficulty("MEDIUM");
    }

    @Test
    @DisplayName("GET /activities/filter should accept query params")
    void filterEndpoint_acceptsParams() throws Exception {
        when(service.filterActivities(any(), any())).thenReturn(Collections.singletonList(sample));

        mockMvc.perform(get("/activities/filter?type=MEMORY&difficulty=MEDIUM").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Sample"));

        verify(service).filterActivities(any(), any());
    }
}
