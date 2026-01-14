package com.example.slaughterhouse;

import com.example.slaughterhouse.application.dto.ProductDto;
import com.example.slaughterhouse.domain.entities.Product;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.enums.LotStatus;
import com.example.slaughterhouse.domain.enums.ProductStatus;
import com.example.slaughterhouse.domain.enums.ProductType;
import com.example.slaughterhouse.infrastructure.persistance.repository.ProductRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ProductRESTTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private SlaughterLotRepository slaughterLotRepository;
    
    private SlaughterLot testLot;
    private String baseUrl;
    
    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/products";
        
        // Clean up
        productRepository.deleteAll();
        slaughterLotRepository.deleteAll();
        
        // Create test slaughter lot
        testLot = new SlaughterLot();
        testLot.setLotNumber("LOT-TEST-001");
        testLot.setBreed("Test Breed");
        testLot.setSlaughterDate(LocalDate.now());
        testLot.setTotalChickens(1000);
        testLot.setCurrentQuantity(1000);
        testLot.setStatus(LotStatus.RECEIVED);
        testLot.setIsActive(true);
        testLot = slaughterLotRepository.save(testLot);
    }
    
    @Test
    public void testCreateProduct() {
        ProductDto productDto = new ProductDto();
        productDto.setSlaughterLotId(testLot.getId());
        productDto.setProductType(ProductType.WHOLE_CHICKEN);
        productDto.setCut("Whole");
        productDto.setWeightValue(2.5);
        productDto.setWeightUnit("kg");
        productDto.setProductionDate(LocalDate.now());
        productDto.setStatus(ProductStatus.PRODUCED);
        productDto.setBatchNumber("BATCH-001");
        productDto.setInspectionPassed(false);
        
        ResponseEntity<ProductDto> response = restTemplate.postForEntity(
            baseUrl, 
            productDto, 
            ProductDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(ProductType.WHOLE_CHICKEN, response.getBody().getProductType());
    }
    
    @Test
    public void testGetAllProducts() {
        // Create test product
        Product product = new Product();
        product.setSlaughterLot(testLot);
        product.setProductType(ProductType.BREAST);
        product.setCut("Breast Fillet");
        product.setProductionDate(LocalDate.now());
        product.setStatus(ProductStatus.PRODUCED);
        product.setInspectionPassed(false);
        product.setIsActive(true);
        productRepository.save(product);
        
        ResponseEntity<ProductDto[]> response = restTemplate.getForEntity(
            baseUrl, 
            ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }
    
    @Test
    public void testGetProductById() {
        // Create test product
        Product product = new Product();
        product.setSlaughterLot(testLot);
        product.setProductType(ProductType.THIGH);
        product.setCut("Thigh");
        product.setProductionDate(LocalDate.now());
        product.setStatus(ProductStatus.PRODUCED);
        product.setInspectionPassed(false);
        product.setIsActive(true);
        product = productRepository.save(product);
        
        ResponseEntity<ProductDto> response = restTemplate.getForEntity(
            baseUrl + "/" + product.getId(), 
            ProductDto.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(product.getId(), response.getBody().getId());
        assertEquals(ProductType.THIGH, response.getBody().getProductType());
    }
    
    @Test
    public void testGetProductsByStatus() {
        // Create test product
        Product product = new Product();
        product.setSlaughterLot(testLot);
        product.setProductType(ProductType.WING);
        product.setCut("Wing");
        product.setProductionDate(LocalDate.now());
        product.setStatus(ProductStatus.APPROVED);
        product.setInspectionPassed(true);
        product.setIsActive(true);
        productRepository.save(product);
        
        ResponseEntity<ProductDto[]> response = restTemplate.getForEntity(
            baseUrl + "/status/" + ProductStatus.APPROVED, 
            ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertEquals(ProductStatus.APPROVED, response.getBody()[0].getStatus());
    }
}
