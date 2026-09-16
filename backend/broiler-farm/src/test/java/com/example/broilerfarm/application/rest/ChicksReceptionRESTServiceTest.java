package com.example.broilerfarm.application.rest;

import com.example.broilerfarm.application.ChicksReceptionApplicationService;
import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.application.dto.ChicksReceptionLineDto;
import com.example.broilerfarm.application.transformation.ChicksReceptionTransformationService;
import com.example.broilerfarm.domain.entities.ChicksReception;
import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.broilerfarm.domain.enums.ReceptionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST Controller tests for ChicksReceptionRESTService.
 * Tests all HTTP endpoints and error handling.
 */
@WebMvcTest(ChicksReceptionRESTService.class)
@AutoConfigureMockMvc(addFilters = false)  // Disable Spring Security for tests
@DisplayName("ChicksReceptionRESTService Tests")
class ChicksReceptionRESTServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChicksReceptionApplicationService applicationService;

    @MockBean
    private ChicksReceptionTransformationService transformationService;

    private ChickReceptionDto validReceptionDto;
    private ChicksReception validReception;

    @BeforeEach
    void setUp() {
        // Setup test data
        ChicksReceptionLineDto lineDto = new ChicksReceptionLineDto();
        lineDto.setPoultryHouseId(1L);
        lineDto.setQuantity(5000);
        lineDto.setChicksAlive(4950);
        lineDto.setChicksDOA(30);
        lineDto.setChicksWeak(20);
        lineDto.setQualityGrade(QualityGrade.A);
        lineDto.setBreed("Ross 308");
        lineDto.setHatcherySource("Hatchery ABC");

        validReceptionDto = new ChickReceptionDto();
        validReceptionDto.setId(1L);
        validReceptionDto.setFarmId(1L);
        validReceptionDto.setEmployeeid(1L);
        validReceptionDto.setReceptionDate(LocalDateTime.now());
        validReceptionDto.setTransportConditions("Good");
        validReceptionDto.setTruckInfo("Truck-001");
        validReceptionDto.setReceptionStatus(ReceptionStatus.DRAFT);
        validReceptionDto.setLines(List.of(lineDto));

        validReception = new ChicksReception();
        validReception.setId(1L);
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("POST /api/chicks-receptions - Success")
    void createReception_Success() throws Exception {
        // Arrange
        when(applicationService.createReception(any(ChickReceptionDto.class)))
                .thenReturn(validReception);
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.farmId").value(1))
                .andExpect(jsonPath("$.receptionStatus").value("DRAFT"));

        verify(applicationService, times(1)).createReception(any(ChickReceptionDto.class));
    }

    @Test
    @DisplayName("POST /api/chicks-receptions - Missing farmId")
    void createReception_MissingFarmId_BadRequest() throws Exception {
        // Arrange
        validReceptionDto.setFarmId(null);

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isBadRequest());

        verify(applicationService, never()).createReception(any());
    }

    @Test
    @DisplayName("POST /api/chicks-receptions - Missing employeeId")
    void createReception_MissingEmployeeId_BadRequest() throws Exception {
        // Arrange
        validReceptionDto.setEmployeeid(null);

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isBadRequest());

        verify(applicationService, never()).createReception(any());
    }

    @Test
    @DisplayName("POST /api/chicks-receptions - Empty lines")
    void createReception_EmptyLines_BadRequest() throws Exception {
        // Arrange
        validReceptionDto.setLines(List.of());

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isBadRequest());

        verify(applicationService, never()).createReception(any());
    }

    @Test
    @DisplayName("POST /api/chicks-receptions - Service throws IllegalArgumentException")
    void createReception_ServiceThrowsException_BadRequest() throws Exception {
        // Arrange
        when(applicationService.createReception(any(ChickReceptionDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid data"));

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isBadRequest());
    }

    // ==================== READ TESTS ====================

    @Test
    @DisplayName("GET /api/chicks-receptions/{id} - Success")
    void getById_Success() throws Exception {
        // Arrange
        when(applicationService.getReceptionById(1L))
                .thenReturn(validReceptionDto);

        // Act & Assert
        mockMvc.perform(get("/api/chicks-receptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.farmId").value(1));

        verify(applicationService, times(1)).getReceptionById(1L);
    }

    @Test
    @DisplayName("GET /api/chicks-receptions/{id} - Not Found")
    void getById_NotFound() throws Exception {
        // Arrange
        when(applicationService.getReceptionById(999L))
                .thenThrow(new IllegalArgumentException("Reception not found"));

        // Act & Assert
        mockMvc.perform(get("/api/chicks-receptions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/chicks-receptions - Success")
    void getAll_Success() throws Exception {
        // Arrange
        List<ChickReceptionDto> receptions = Arrays.asList(validReceptionDto);
        when(applicationService.getAllReceptions())
                .thenReturn(receptions);

        // Act & Assert
        mockMvc.perform(get("/api/chicks-receptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(applicationService, times(1)).getAllReceptions();
    }

    @Test
    @DisplayName("GET /api/chicks-receptions/farm/{farmId} - Success")
    void getByFarmId_Success() throws Exception {
        // Arrange
        List<ChickReceptionDto> receptions = Arrays.asList(validReceptionDto);
        when(applicationService.getReceptionsByFarm(1L))
                .thenReturn(receptions);

        // Act & Assert
        mockMvc.perform(get("/api/chicks-receptions/farm/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].farmId").value(1));

        verify(applicationService, times(1)).getReceptionsByFarm(1L);
    }

    @Test
    @DisplayName("GET /api/chicks-receptions/farm/{farmId} - Farm Not Found")
    void getByFarmId_NotFound() throws Exception {
        // Arrange
        when(applicationService.getReceptionsByFarm(999L))
                .thenThrow(new IllegalArgumentException("Farm not found"));

        // Act & Assert
        mockMvc.perform(get("/api/chicks-receptions/farm/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("PUT /api/chicks-receptions/{id} - Success")
    void updateReception_Success() throws Exception {
        // Arrange
        validReceptionDto.setTransportConditions("Excellent");
        when(applicationService.updateReception(anyLong(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(validReceptionDto);

        // Act & Assert
        mockMvc.perform(put("/api/chicks-receptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transportConditions").value("Excellent"));

        verify(applicationService, times(1))
                .updateReception(anyLong(), any(), anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PUT /api/chicks-receptions/{id} - Reception Not Found")
    void updateReception_NotFound() throws Exception {
        // Arrange
        when(applicationService.updateReception(anyLong(), any(), anyLong(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Reception not found"));

        // Act & Assert
        mockMvc.perform(put("/api/chicks-receptions/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/chicks-receptions/{id} - Already Finalized")
    void updateReception_AlreadyFinalized_Conflict() throws Exception {
        // Arrange
        when(applicationService.updateReception(anyLong(), any(), anyLong(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Cannot update finalized reception"));

        // Act & Assert
        mockMvc.perform(put("/api/chicks-receptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isConflict());
    }

    // ==================== FINALIZE TESTS ====================

    @Test
    @DisplayName("POST /api/chicks-receptions/{id}/finalize - Success")
    void finalizeReception_Success() throws Exception {
        // Arrange
        validReceptionDto.setReceptionStatus(ReceptionStatus.CONFIRMED);
        when(applicationService.finalizeReception(1L))
                .thenReturn(validReceptionDto);

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions/1/finalize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receptionStatus").value("CONFIRMED"));

        verify(applicationService, times(1)).finalizeReception(1L);
    }

    @Test
    @DisplayName("POST /api/chicks-receptions/{id}/finalize - Reception Not Found")
    void finalizeReception_NotFound() throws Exception {
        // Arrange
        when(applicationService.finalizeReception(999L))
                .thenThrow(new IllegalArgumentException("Reception not found"));

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions/999/finalize"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/chicks-receptions/{id}/finalize - Already Finalized")
    void finalizeReception_AlreadyFinalized_Conflict() throws Exception {
        // Arrange
        when(applicationService.finalizeReception(1L))
                .thenThrow(new IllegalStateException("Can only finalize DRAFT reception"));

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions/1/finalize"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/chicks-receptions/{id}/finalize - Missing Poultry House")
    void finalizeReception_MissingPoultryHouse_Conflict() throws Exception {
        // Arrange
        when(applicationService.finalizeReception(1L))
                .thenThrow(new IllegalStateException("All lines must have poultry house assigned"));

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions/1/finalize"))
                .andExpect(status().isConflict());
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("DELETE /api/chicks-receptions/{id} - Success")
    void deleteReception_Success() throws Exception {
        // Arrange
        doNothing().when(applicationService).deleteReception(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/chicks-receptions/1"))
                .andExpect(status().isNoContent());

        verify(applicationService, times(1)).deleteReception(1L);
    }

    @Test
    @DisplayName("DELETE /api/chicks-receptions/{id} - Not Found")
    void deleteReception_NotFound() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Reception not found"))
                .when(applicationService).deleteReception(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/chicks-receptions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/chicks-receptions/{id} - Cannot Delete Finalized")
    void deleteReception_Finalized_Conflict() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Cannot delete finalized reception"))
                .when(applicationService).deleteReception(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/chicks-receptions/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/chicks-receptions/{id} - Has Associated Data")
    void deleteReception_HasAssociatedData_Conflict() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Cannot delete reception with lots that have data"))
                .when(applicationService).deleteReception(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/chicks-receptions/1"))
                .andExpect(status().isConflict());
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("POST /api/chicks-receptions - Internal Server Error")
    void createReception_InternalError() throws Exception {
        // Arrange
        when(applicationService.createReception(any(ChickReceptionDto.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/chicks-receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceptionDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/chicks-receptions/{id} - Internal Server Error")
    void getById_InternalError() throws Exception {
        // Arrange
        when(applicationService.getReceptionById(1L))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/chicks-receptions/1"))
                .andExpect(status().isInternalServerError());
    }
}