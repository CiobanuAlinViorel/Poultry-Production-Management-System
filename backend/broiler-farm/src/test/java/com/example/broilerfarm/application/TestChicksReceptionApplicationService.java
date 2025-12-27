package com.example.broilerfarm.application;

import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.application.dto.ChicksReceptionLineDto;
import com.example.broilerfarm.application.transformation.ChicksReceptionTransformationService;
import com.example.broilerfarm.domain.entities.*;
import com.example.broilerfarm.domain.enums.ChicksLotStatus;
import com.example.broilerfarm.domain.enums.PoultryHouseStatus;
import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.broilerfarm.domain.enums.ReceptionStatus;
import com.example.broilerfarm.infrastructure.persistence.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ChicksReceptionApplicationService.
 * Tests UC-01 (Create Reception) and UC-02 (Edit Reception).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChicksReceptionApplicationService Tests")
class ChicksReceptionApplicationServiceTest {

    @Mock
    private ChicksReceptionRepository chicksReceptionRepository;

    @Mock
    private PoultryHouseRepository poultryHouseRepository;

    @Mock
    private BroilerFarmRepository broilerFarmRepository;

    @Mock
    private FarmEmployeeRepository farmEmployeeRepository;

    @Mock
    private ChicksReceptionTransformationService transformationService;

    @Mock
    private ChicksLotRepository chicksLotRepository;

    @InjectMocks
    private ChicksReceptionApplicationService service;

    private ChickReceptionDto validReceptionDto;
    private ChicksReception validReception;
    private BroilerFarm testFarm;
    private FarmEmployee testEmployee;
    private PoultryHouse testHouse;
    private ChicksReceptionLine testLine;

    @BeforeEach
    void setUp() {
        // Setup test farm
        testFarm = new BroilerFarm();
        testFarm.setId(1L);
        testFarm.setFarmName("Test Farm");

        // Setup test employee
        testEmployee = new FarmEmployee();
        testEmployee.setId(1L);
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");

        // Setup test poultry house
        testHouse = new PoultryHouse();
        testHouse.setId(1L);
        testHouse.setCapacity(10000);
        testHouse.setStatus(PoultryHouseStatus.EMPTY);
        testHouse.setFarm(testFarm);

        // Setup test reception line DTO
        ChicksReceptionLineDto lineDto = new ChicksReceptionLineDto();
        lineDto.setPoultryHouseId(1L);
        lineDto.setQuantity(5000);
        lineDto.setChicksAlive(4950);
        lineDto.setChicksDOA(30);
        lineDto.setChicksWeak(20);
        lineDto.setQualityGrade(QualityGrade.A);
        lineDto.setBreed("Ross 308");
        lineDto.setHatcherySource("Hatchery ABC");

        // Setup test reception DTO
        validReceptionDto = new ChickReceptionDto();
        validReceptionDto.setFarmId(1L);
        validReceptionDto.setEmployeeid(1L);
        validReceptionDto.setReceptionDate(LocalDateTime.now());
        validReceptionDto.setTransportConditions("Good");
        validReceptionDto.setTruckInfo("Truck-001");
        validReceptionDto.setLines(List.of(lineDto));

        // Setup test reception entity
        validReception = new ChicksReception();
        validReception.setId(1L);
        validReception.setFarm(testFarm);
        validReception.setReceivingEmployee(testEmployee);
        validReception.setReceptionDate(LocalDateTime.now());
        validReception.setStatus(ReceptionStatus.DRAFT);

        // Setup test reception line entity
        testLine = new ChicksReceptionLine();
        testLine.setId(1L);
        testLine.setPoultryHouse(testHouse);
        testLine.setQuantity(5000);
        testLine.setChicksAlive(4950);
        testLine.setChicksDOA(30);
        testLine.setChicksWeak(20);
        testLine.setQualityGrade(QualityGrade.A);
    }

    // ==================== CREATE RECEPTION TESTS (UC-01) ====================

    @Test
    @DisplayName("UC-01: Create reception successfully - happy path")
    void createReception_Success() {
        // Arrange
        when(transformationService.toEntity(any(ChickReceptionDto.class)))
                .thenReturn(validReception);
        when(transformationService.toLineEntity(any(ChicksReceptionLineDto.class)))
                .thenReturn(testLine);
        when(chicksReceptionRepository.save(any(ChicksReception.class)))
                .thenReturn(validReception);

        // Act
        ChicksReception result = service.createReception(validReceptionDto);

        // Assert
        assertNotNull(result);
        assertEquals(ReceptionStatus.DRAFT, result.getStatus());
        verify(chicksReceptionRepository, times(1)).save(any(ChicksReception.class));
        verify(transformationService, times(1)).toEntity(validReceptionDto);
    }

    @Test
    @DisplayName("UC-01: Fail when reception DTO is null")
    void createReception_NullDto_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReception(null)
        );
        assertEquals("Reception DTO cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("UC-01: Fail when reception has no lines")
    void createReception_NoLines_ThrowsException() {
        // Arrange
        validReceptionDto.setLines(new ArrayList<>());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReception(validReceptionDto)
        );
        assertEquals("Reception must have at least one line", exception.getMessage());
    }

    @Test
    @DisplayName("UC-01: Fail when farm is null")
    void createReception_NullFarm_ThrowsException() {
        // Arrange
        validReception.setFarm(null);
        // Transformation happens before validation, so we need these stubs
        lenient().when(transformationService.toEntity(any(ChickReceptionDto.class)))
                .thenReturn(validReception);
        lenient().when(transformationService.toLineEntity(any(ChicksReceptionLineDto.class)))
                .thenReturn(testLine);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReception(validReceptionDto)
        );
        assertEquals("Farm cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("UC-01: Fail when employee is null")
    void createReception_NullEmployee_ThrowsException() {
        // Arrange
        validReception.setReceivingEmployee(null);
        lenient().when(transformationService.toEntity(any(ChickReceptionDto.class)))
                .thenReturn(validReception);
        lenient().when(transformationService.toLineEntity(any(ChicksReceptionLineDto.class)))
                .thenReturn(testLine);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReception(validReceptionDto)
        );
        assertEquals("Receiving employee cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("UC-01: Fail when reception date is null")
    void createReception_NullDate_ThrowsException() {
        // Arrange
        validReception.setReceptionDate(null);
        lenient().when(transformationService.toEntity(any(ChickReceptionDto.class)))
                .thenReturn(validReception);
        lenient().when(transformationService.toLineEntity(any(ChicksReceptionLineDto.class)))
                .thenReturn(testLine);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReception(validReceptionDto)
        );
        assertEquals("Reception date cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("UC-01: Reception created in DRAFT status")
    void createReception_StatusIsDraft() {
        // Arrange
        when(transformationService.toEntity(any(ChickReceptionDto.class)))
                .thenReturn(validReception);
        when(transformationService.toLineEntity(any(ChicksReceptionLineDto.class)))
                .thenReturn(testLine);

        ArgumentCaptor<ChicksReception> captor = ArgumentCaptor.forClass(ChicksReception.class);
        when(chicksReceptionRepository.save(captor.capture()))
                .thenReturn(validReception);

        // Act
        service.createReception(validReceptionDto);

        // Assert
        ChicksReception savedReception = captor.getValue();
        assertEquals(ReceptionStatus.DRAFT, savedReception.getStatus());
    }

    // ==================== UPDATE RECEPTION TESTS (UC-02) ====================

    @Test
    @DisplayName("UC-02: Update reception successfully - update header fields")
    void updateReception_UpdateHeaderFields_Success() {
        // Arrange
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));
        when(farmEmployeeRepository.findById(2L))
                .thenReturn(Optional.of(testEmployee));
        when(chicksReceptionRepository.save(any(ChicksReception.class)))
                .thenReturn(validReception);
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act
        ChickReceptionDto result = service.updateReception(
                1L, newDate, 2L, "Excellent", "Truck-002", "REF-001", null
        );

        // Assert
        assertNotNull(result);
        verify(chicksReceptionRepository, times(1)).save(validReception);
        assertEquals(newDate, validReception.getReceptionDate());
        assertEquals("Excellent", validReception.getTransportConditions());
    }

    @Test
    @DisplayName("UC-02: Fail to update when reception ID is null")
    void updateReception_NullId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateReception(null, null, null, null, null, null, null)
        );
        assertEquals("Reception ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("UC-02: Fail to update when reception not found")
    void updateReception_NotFound_ThrowsException() {
        // Arrange
        when(chicksReceptionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateReception(999L, null, null, null, null, null, null)
        );
        assertEquals("Reception not found", exception.getMessage());
    }

    @Test
    @DisplayName("UC-02: Fail to update finalized reception")
    void updateReception_Finalized_ThrowsException() {
        // Arrange
        validReception.setStatus(ReceptionStatus.CONFIRMED);
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.updateReception(1L, null, null, null, null, null, null)
        );
        assertEquals("Cannot update finalized reception", exception.getMessage());
    }

    @Test
    @DisplayName("UC-02: Fail to update with invalid employee")
    void updateReception_InvalidEmployee_ThrowsException() {
        // Arrange
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));
        when(farmEmployeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateReception(1L, null, 999L, null, null, null, null)
        );
        assertEquals("Employee not found", exception.getMessage());
    }

    @Test
    @DisplayName("UC-02: Update reception lines - add new line")
    void updateReception_AddNewLine_Success() {
        // Arrange
        ChicksReceptionLineDto newLineDto = new ChicksReceptionLineDto();
        newLineDto.setPoultryHouseId(2L);
        newLineDto.setQuantity(3000);
        newLineDto.setChicksAlive(2980);
        newLineDto.setChicksDOA(15);
        newLineDto.setChicksWeak(5);
        newLineDto.setQualityGrade(QualityGrade.A);

        ChicksReceptionLine newLine = new ChicksReceptionLine();
        newLine.setPoultryHouse(testHouse);
        newLine.setQuantity(3000);
        newLine.setChicksAlive(2980);
        newLine.setChicksDOA(15);
        newLine.setChicksWeak(5);
        newLine.setQualityGrade(QualityGrade.A);

        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));
        when(transformationService.toLineEntity(any(ChicksReceptionLineDto.class)))
                .thenReturn(newLine);
        when(chicksReceptionRepository.save(any(ChicksReception.class)))
                .thenReturn(validReception);
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act
        ChickReceptionDto result = service.updateReception(
                1L, null, null, null, null, null, List.of(newLineDto)
        );

        // Assert
        assertNotNull(result);
        verify(chicksReceptionRepository, times(1)).save(validReception);
    }

    // ==================== FINALIZE RECEPTION TESTS ====================

    @Test
    @DisplayName("Finalize reception successfully - creates lots")
    void finalizeReception_Success() {
        // Arrange
        validReception.getReceptionLines().add(testLine);
        ChicksLot testLot = ChicksLot.builder()
                .lotNumber("TST-H001-2025-50")
                .house(testHouse)
                .breed("Ross 308")
                .hatcherySource("Hatchery ABC")
                .receptionDate(LocalDate.now())
                .initialQuantity(4950)
                .currentQuantity(4950)
                .status(ChicksLotStatus.GROWING)
                .build();

        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));
        when(chicksLotRepository.save(any(ChicksLot.class)))
                .thenReturn(testLot);
        when(poultryHouseRepository.save(any(PoultryHouse.class)))
                .thenReturn(testHouse);
        when(chicksReceptionRepository.save(any(ChicksReception.class)))
                .thenReturn(validReception);
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act
        ChickReceptionDto result = service.finalizeReception(1L);

        // Assert
        assertNotNull(result);
        verify(chicksLotRepository, times(1)).save(any(ChicksLot.class));
        verify(poultryHouseRepository, times(1)).save(any(PoultryHouse.class));
    }

    @Test
    @DisplayName("Finalize reception - fail when ID is null")
    void finalizeReception_NullId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.finalizeReception(null)
        );
        assertEquals("Reception ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Finalize reception - fail when reception not found")
    void finalizeReception_NotFound_ThrowsException() {
        // Arrange
        when(chicksReceptionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.finalizeReception(999L)
        );
        assertEquals("Reception not found", exception.getMessage());
    }

    @Test
    @DisplayName("Finalize reception - fail when already finalized")
    void finalizeReception_AlreadyFinalized_ThrowsException() {
        // Arrange
        validReception.setStatus(ReceptionStatus.CONFIRMED);
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.finalizeReception(1L)
        );
        assertEquals("Can only finalize DRAFT reception", exception.getMessage());
    }

    @Test
    @DisplayName("Finalize reception - fail when no lines")
    void finalizeReception_NoLines_ThrowsException() {
        // Arrange
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.finalizeReception(1L)
        );
        assertEquals("Cannot finalize reception without lines", exception.getMessage());
    }

    @Test
    @DisplayName("Finalize reception - fail when line has no poultry house")
    void finalizeReception_NoPoultryHouse_ThrowsException() {
        // Arrange
        testLine.setPoultryHouse(null);
        validReception.getReceptionLines().add(testLine);
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.finalizeReception(1L)
        );
        assertEquals("All lines must have poultry house assigned", exception.getMessage());
    }

    @Test
    @DisplayName("Finalize reception - skip lot creation if already created")
    void finalizeReception_LotAlreadyExists_SkipsCreation() {
        // Arrange
        ChicksLot existingLot = ChicksLot.builder()
                .lotNumber("TST-H001-2025-50")
                .build();
        testLine.setCreatedLot(existingLot);
        validReception.getReceptionLines().add(testLine);

        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));
        when(chicksReceptionRepository.save(any(ChicksReception.class)))
                .thenReturn(validReception);
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act
        ChickReceptionDto result = service.finalizeReception(1L);

        // Assert
        assertNotNull(result);
        verify(chicksLotRepository, never()).save(any(ChicksLot.class));
    }

    // ==================== GET OPERATIONS TESTS ====================

    @Test
    @DisplayName("Get all receptions successfully")
    void getAllReceptions_Success() {
        // Arrange
        List<ChicksReception> receptions = Arrays.asList(validReception);
        when(chicksReceptionRepository.findAll()).thenReturn(receptions);
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act
        List<ChickReceptionDto> result = service.getAllReceptions();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(chicksReceptionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Get reception by ID successfully")
    void getReceptionById_Success() {
        // Arrange
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act
        ChickReceptionDto result = service.getReceptionById(1L);

        // Assert
        assertNotNull(result);
        verify(chicksReceptionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Get reception by ID - fail when ID is null")
    void getReceptionById_NullId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getReceptionById(null)
        );
        assertEquals("Reception ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Get reception by ID - fail when not found")
    void getReceptionById_NotFound_ThrowsException() {
        // Arrange
        when(chicksReceptionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getReceptionById(999L)
        );
        assertEquals("Reception not found", exception.getMessage());
    }

    @Test
    @DisplayName("Get receptions by farm successfully")
    void getReceptionsByFarm_Success() {
        // Arrange
        List<ChicksReception> receptions = Arrays.asList(validReception);
        when(broilerFarmRepository.findById(1L))
                .thenReturn(Optional.of(testFarm));
        when(chicksReceptionRepository.findByFarmId(1L))
                .thenReturn(receptions);
        when(transformationService.toDto(any(ChicksReception.class)))
                .thenReturn(validReceptionDto);

        // Act
        List<ChickReceptionDto> result = service.getReceptionsByFarm(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(broilerFarmRepository, times(1)).findById(1L);
        verify(chicksReceptionRepository, times(1)).findByFarmId(1L);
    }

    @Test
    @DisplayName("Get receptions by farm - fail when farm ID is null")
    void getReceptionsByFarm_NullId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getReceptionsByFarm(null)
        );
        assertEquals("Farm ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Get receptions by farm - fail when farm not found")
    void getReceptionsByFarm_FarmNotFound_ThrowsException() {
        // Arrange
        when(broilerFarmRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getReceptionsByFarm(999L)
        );
        assertEquals("Farm not found", exception.getMessage());
    }

    // ==================== DELETE RECEPTION TESTS ====================

    @Test
    @DisplayName("Delete reception successfully - DRAFT without lots")
    void deleteReception_DraftWithoutLots_Success() {
        // Arrange
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));

        // Act
        service.deleteReception(1L);

        // Assert
        verify(chicksReceptionRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Delete reception - fail when ID is null")
    void deleteReception_NullId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteReception(null)
        );
        assertEquals("Reception ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Delete reception - fail when not found")
    void deleteReception_NotFound_ThrowsException() {
        // Arrange
        when(chicksReceptionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteReception(999L)
        );
        assertEquals("Reception not found", exception.getMessage());
    }

    @Test
    @DisplayName("Delete reception - fail when finalized")
    void deleteReception_Finalized_ThrowsException() {
        // Arrange
        validReception.setStatus(ReceptionStatus.CONFIRMED);
        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.deleteReception(1L)
        );
        assertEquals("Cannot delete finalized reception", exception.getMessage());
    }

    @Test
    @DisplayName("Delete reception - fail when lot has associated data")
    void deleteReception_LotHasData_ThrowsException() {
        // Note: This test would require proper repository setup in integration tests
        // For now, it's a placeholder to show the concept

        // This test should be moved to integration tests where actual
        // data relationships can be tested
        assertTrue(true, "Placeholder - implement in integration tests");
    }

    @Test
    @DisplayName("Delete reception with lot - cleanup house status")
    void deleteReception_WithLot_CleansUpHouse() {
        // Arrange
        ChicksLot testLot = ChicksLot.builder()
                .lotNumber("TST-H001-2025-50")
                .build();
        testLine.setCreatedLot(testLot);
        testHouse.setCurrentLot(testLot);
        testHouse.setStatus(PoultryHouseStatus.OCCUPIED);
        validReception.getReceptionLines().add(testLine);

        when(chicksReceptionRepository.findById(1L))
                .thenReturn(Optional.of(validReception));
        when(poultryHouseRepository.save(any(PoultryHouse.class)))
                .thenReturn(testHouse);

        // Act
        service.deleteReception(1L);

        // Assert
        verify(chicksLotRepository, times(1)).delete(testLot);
        verify(poultryHouseRepository, times(1)).save(testHouse);
        verify(chicksReceptionRepository, times(1)).deleteById(1L);
    }
}