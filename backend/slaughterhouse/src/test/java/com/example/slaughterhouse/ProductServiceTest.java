package com.example.slaughterhouse;

import com.example.slaughterhouse.application.service.ProductService;
import com.example.slaughterhouse.domain.entities.Product;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.enums.ProductStatus;
import com.example.slaughterhouse.domain.enums.ProductType;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private SlaughterLotRepository slaughterLotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @Rollback(false) // <--- păstrează datele după rulare
    void testMarkProductAsInspected() {
        // Creăm un SlaughterLot
        SlaughterLot lot = new SlaughterLot();
        lot.setLotNumber("LOT-001");
        lot.setSlaughterDate(LocalDate.now());
        lot.setTotalChickens(100);
        lot.setCurrentQuantity(100); // necesar pentru coloana not-null
        lot.setIsActive(true);
        slaughterLotRepository.save(lot);

        // Creăm un Product
        Product product = new Product();
        product.setSlaughterLot(lot);
        product.setProductType(ProductType.BREAST);
        product.setProductionDate(LocalDate.now()); // necesar pentru coloana not-null
        product.setStatus(ProductStatus.PRODUCED);
        product.setInspectionPassed(false);
        productRepository.save(product);

        // Testăm metoda service
        productService.markProductAsInspected(product.getId(), true);

        // Verificăm rezultatul
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getInspectionPassed()).isTrue();
        assertThat(updatedProduct.getStatus()).isEqualTo(ProductStatus.APPROVED);
    }
}
