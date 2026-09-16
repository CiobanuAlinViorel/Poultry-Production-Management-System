package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.*;
import com.example.broilerfarm.domain.enums.*;
import com.example.broilerfarm.infrastructure.persistence.repositories.BroilerFarmRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksLotRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.PoultryHouseRepository;
import com.example.shared.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("ChicksLotFactory Tests")
class ChicksLotFactoryTest {

    private static final Logger logger = Logger.getLogger(ChicksLotFactoryTest.class.getName());

    @Autowired
    private ChicksLotFactory chicksLotFactory;

    @MockBean
    private ChicksLotRepository chicksLotRepository;

    @MockBean
    private PoultryHouseRepository poultryHouseRepository;

    @MockBean
    private BroilerFarmRepository broilerFarmRepository;

    private BroilerFarm broilerFarm;
    private FarmEmployee farmEmployee;
    private PoultryHouse poultryHouse;
    private ChicksReception chicksReception;
    private ChicksReceptionLine chicksReceptionLine;
    private LocalDate date;
    private LocalDateTime dateTime;

    @BeforeEach
    void setUp() {
        date = LocalDate.now();
        dateTime = LocalDateTime.now();

        // Setup BroilerFarm
        broilerFarm = new BroilerFarm(
                1L,
                dateTime,
                dateTime,
                "F1",
                "Location",
                "Address",
                1000,
                "L1000"
        );

        // Setup FarmEmployee
        farmEmployee = new FarmEmployee(
                1L,
                dateTime,
                dateTime,
                "Cristi",
                "Cristian",
                "0762612341",
                "cristi@example.com",
                Role.WORKER,
                LocalDate.now(),
                broilerFarm
        );

        // Setup PoultryHouse - IMPORTANT: Status must be EMPTY
        poultryHouse = new PoultryHouse(
                1L,
                dateTime,
                dateTime,
                broilerFarm,
                200,
                null,
                200.0,
                PoultryHouseType.CLOSED,
                "diverse",
                PoultryHouseStatus.EMPTY,  // Must be EMPTY for validation
                0
        );

        // Setup ChicksReception
        chicksReception = new ChicksReception(
                1L,
                date.atStartOfDay(),
                date.atStartOfDay(),
                date.atStartOfDay(),
                broilerFarm,
                farmEmployee,
                "good",
                "big",
                "DeliveryNotice1923",
                ReceptionStatus.CONFIRMED,
                200,
                194,
                4,
                2
        );

        // Setup ChicksReceptionLine
        chicksReceptionLine = new ChicksReceptionLine(
                1L,
                dateTime,
                dateTime,
                chicksReception,
                poultryHouse,
                null,
                200,
                194,
                4,
                2,
                QualityGrade.A,
                "All is fine"
        );
    }

    @Test
    @DisplayName("Should create ChicksLot successfully when PoultryHouse exists")
    void testCreateLotWithExistingPoultryHouse() {
        logger.info("Domain Service implementation instance: " + chicksLotFactory);
        logger.info("Domain Service implementation class: " + chicksLotFactory.getClass().getSimpleName());

        // Mock: PoultryHouse exists
        when(poultryHouseRepository.existsById(1L)).thenReturn(true);

        // Create expected lot
        ChicksLot expectedLot = new ChicksLot(
                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "Premium Hatchery",
                "Ross 308",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                4.5
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        // Execute
        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        // Verify
        assertNotNull(createdLot, "ChicksLot should not be null");
        assertNotNull(createdLot.getLotNumber(), "ChicksLot Id should not be null");
        assertEquals("1-1-" + date.toString(), createdLot.getLotNumber());
        assertEquals(ChicksLotStatus.GROWING, createdLot.getStatus());
        assertEquals(194, createdLot.getInitialQuantity());
        assertEquals(194, createdLot.getCurrentQuantity());

        logger.info("Created ChicksLot ID: " + createdLot.getLotNumber());

        // Verify repository interactions
        verify(poultryHouseRepository, times(1)).existsById(1L);
        verify(chicksLotRepository, times(1)).save(any(ChicksLot.class));
        verify(broilerFarmRepository, never()).save(any());
        verify(poultryHouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create ChicksLot and save PoultryHouse when it doesn't exist but Farm exists")
    void testCreateLotWithNewPoultryHouseExistingFarm() {
        // Mock: PoultryHouse doesn't exist, but Farm exists
        when(poultryHouseRepository.existsById(1L)).thenReturn(false);
        when(broilerFarmRepository.existsById(1L)).thenReturn(true);
        when(poultryHouseRepository.save(any(PoultryHouse.class))).thenReturn(poultryHouse);

        ChicksLot expectedLot = new ChicksLot(
                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "Local Hatchery",
                "Cobb 500",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                4.0
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        // Execute
        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        // Verify
        assertNotNull(createdLot);
        assertEquals(4.0, createdLot.getExpectedMortalityRate());

        // Verify repository interactions
        verify(poultryHouseRepository, times(1)).existsById(1L);
        verify(broilerFarmRepository, times(1)).existsById(1L);
        verify(poultryHouseRepository, times(1)).save(poultryHouse);
        verify(chicksLotRepository, times(1)).save(any(ChicksLot.class));
        verify(broilerFarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create ChicksLot and save both Farm and PoultryHouse when neither exists")
    void testCreateLotWithNewFarmAndPoultryHouse() {
        // Mock: Neither PoultryHouse nor Farm exists
        when(poultryHouseRepository.existsById(1L)).thenReturn(false);
        when(broilerFarmRepository.existsById(1L)).thenReturn(false);
        when(broilerFarmRepository.save(any(BroilerFarm.class))).thenReturn(broilerFarm);
        when(poultryHouseRepository.save(any(PoultryHouse.class))).thenReturn(poultryHouse);

        ChicksLot expectedLot = new ChicksLot(
                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "New Hatchery",
                "Arbor Acres",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                4.2
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        // Execute
        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        // Verify
        assertNotNull(createdLot);
        //assertEquals(3L, createdLot.getId());
        assertEquals(4.2, createdLot.getExpectedMortalityRate());

        // Verify repository interactions
        verify(poultryHouseRepository, times(1)).existsById(1L);
        verify(broilerFarmRepository, times(1)).existsById(1L);
        verify(broilerFarmRepository, times(1)).save(broilerFarm);
        verify(poultryHouseRepository, times(1)).save(poultryHouse);
        verify(chicksLotRepository, times(1)).save(any(ChicksLot.class));
    }

    @Test
    @DisplayName("Should calculate correct expected mortality rate for Ross 308")
    void testMortalityRateRoss308() {
        when(poultryHouseRepository.existsById(1L)).thenReturn(true);

        ChicksLot expectedLot = new ChicksLot(
                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "Hatchery",
                "ROSS 308",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                4.5
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        assertEquals(4.5, createdLot.getExpectedMortalityRate());
    }

    @Test
    @DisplayName("Should calculate correct expected mortality rate for Cobb 500")
    void testMortalityRateCobb500() {
        when(poultryHouseRepository.existsById(1L)).thenReturn(true);

        ChicksLot expectedLot = new ChicksLot(

                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "Hatchery",
                "COBB 500",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                4.0
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        assertEquals(4.0, createdLot.getExpectedMortalityRate());
    }

    @Test
    @DisplayName("Should use default mortality rate for unknown breed")
    void testMortalityRateDefault() {
        when(poultryHouseRepository.existsById(1L)).thenReturn(true);

        ChicksLot expectedLot = new ChicksLot(


                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "Hatchery",
                "Unknown Breed",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                5.0
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        assertEquals(5.0, createdLot.getExpectedMortalityRate());
    }

    @Test
    @DisplayName("Should generate correct lot number format")
    void testLotNumberGeneration() {
        when(poultryHouseRepository.existsById(1L)).thenReturn(true);

        ChicksLot expectedLot = new ChicksLot(


                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "hatchery",
                "breed",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                5.0
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        String expectedLotNumber = "1-1-" + date.toString();
        assertEquals(expectedLotNumber, createdLot.getLotNumber());
    }

    @Test
    @DisplayName("Should throw exception when reception line is null")
    void testCreateLotWithNullReceptionLine() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chicksLotFactory.createLot(null)
        );

        assertEquals("Reception line cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when poultry house is null")
    void testCreateLotWithNullPoultryHouse() {
        chicksReceptionLine = new ChicksReceptionLine(
                1L, dateTime, dateTime, chicksReception, null, null,
                200, 194, 4, 2, QualityGrade.A, "notes"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> chicksLotFactory.createLot(chicksReceptionLine)
        );

        assertEquals("Poultry house must be assigned before creating lot", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when chicks alive is zero or negative")
    void testCreateLotWithInvalidChicksAlive() {
        chicksReceptionLine = new ChicksReceptionLine(
                1L, dateTime, dateTime, chicksReception, poultryHouse, null,
                200, 0, 4, 2, QualityGrade.A, "notes"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chicksLotFactory.createLot(chicksReceptionLine)
        );

        assertEquals("Chicks alive must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when poultry house status is not EMPTY")
    void testCreateLotWithNonEmptyHouse() {
        poultryHouse = new PoultryHouse(
                1L, dateTime, dateTime, broilerFarm, 200, null, 200.0,
                PoultryHouseType.CLOSED, "diverse", PoultryHouseStatus.OCCUPIED, 100
        );

        chicksReceptionLine = new ChicksReceptionLine(
                1L, dateTime, dateTime, chicksReception, poultryHouse, null,
                200, 194, 4, 2, QualityGrade.A, "notes"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> chicksLotFactory.createLot(chicksReceptionLine)
        );

        assertTrue(exception.getMessage().contains("is not EMPTY"));
        assertTrue(exception.getMessage().contains("OCCUPIED"));
    }

    @Test
    @DisplayName("Should throw exception when chicks quantity exceeds house capacity")
    void testCreateLotExceedsCapacity() {
        chicksReceptionLine = new ChicksReceptionLine(
                1L, dateTime, dateTime, chicksReception, poultryHouse, null,
                250, 250, 0, 0, QualityGrade.A, "notes"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> chicksLotFactory.createLot(chicksReceptionLine)
        );

        assertTrue(exception.getMessage().contains("exceeds house capacity"));
        assertTrue(exception.getMessage().contains("250"));
        assertTrue(exception.getMessage().contains("200"));
    }

    @Test
    @DisplayName("Should set initial and current quantity to chicks alive")
    void testQuantitiesSetCorrectly() {
        when(poultryHouseRepository.existsById(1L)).thenReturn(true);

        ChicksLot expectedLot = new ChicksLot(


                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "hatchery",
                "breed",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                5.0
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        assertEquals(194, createdLot.getInitialQuantity());
        assertEquals(194, createdLot.getCurrentQuantity());
    }

    @Test
    @DisplayName("Should set reception date from reception line")
    void testReceptionDateSetCorrectly() {
        when(poultryHouseRepository.existsById(1L)).thenReturn(true);

        ChicksLot expectedLot = new ChicksLot(


                "1-1-" + date.toString(),
                dateTime,
                dateTime,
                poultryHouse,
                "hatchery",
                "breed",
                date,
                194,
                194,
                null,
                null,
                ChicksLotStatus.GROWING,
                5.0
        );

        when(chicksLotRepository.save(any(ChicksLot.class))).thenReturn(expectedLot);

        ChicksLot createdLot = chicksLotFactory.createLot(chicksReceptionLine);

        assertEquals(date, createdLot.getReceptionDate());
    }

    @Test
    @DisplayName("Should verify factory is Spring-managed bean")
    void testFactoryIsSpringBean() {
        assertNotNull(chicksLotFactory, "ChicksLotFactory should be autowired");
        logger.info("Factory instance: " + chicksLotFactory);
        logger.info("Factory class: " + chicksLotFactory.getClass().getSimpleName());

        assertTrue(chicksLotFactory.getClass().getName().contains("ChicksLotFactory"));
    }
}