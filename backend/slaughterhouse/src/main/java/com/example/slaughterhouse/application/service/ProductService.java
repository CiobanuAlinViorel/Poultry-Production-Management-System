package com.example.slaughterhouse.application.service;

import com.example.slaughterhouse.domain.entities.Product;
import com.example.slaughterhouse.domain.entities.SlaughterLot;

import java.time.LocalDate;
import java.util.List;

public interface ProductService {
    List<Product> getProductsByLot(SlaughterLot lot);
    List<Product> getProductsByType(String productType);
    int calculateTotalQuantityByLot(SlaughterLot lot);
    List<Product> getProductsProducedBetween(LocalDate startDate, LocalDate endDate);
    void markProductAsInspected(Long productId, boolean passed);
}
