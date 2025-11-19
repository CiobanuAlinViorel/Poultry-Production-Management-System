package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.Product;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.enums.ProductStatus;
import com.example.slaughterhouse.domain.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Găsește produse după lotul de sacrificare
    List<Product> findBySlaughterLot(SlaughterLot slaughterLot);

    // Găsește produse după tipul lor
    List<Product> findByProductType(ProductType productType);

    // Găsește produse după status
    List<Product> findByStatus(ProductStatus status);

    // Găsește produse inspectate
    List<Product> findByInspectionPassedTrue();

    // Găsește produse produse într-o anumită perioadă
    @Query("SELECT p FROM Product p WHERE p.productionDate BETWEEN :startDate AND :endDate")
    List<Product> findProducedBetweenDates(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    // Găsește un produs după numărul de lot
    Optional<Product> findByBatchNumber(String batchNumber);

    // Produse active
    List<Product> findByIsActiveTrue();
    List<Product> findByProductType(String productType);
    List<Product> findByProductionDateBetween(LocalDate startDate, LocalDate endDate);


}
