package com.example.slaughterhouse.application.service;

import com.example.slaughterhouse.domain.entities.Product;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.infrastructure.persistance.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getProductsByLot(SlaughterLot lot) {
        return productRepository.findBySlaughterLot(lot);
    }

    @Override
    public List<Product> getProductsByType(String productType) {
        return productRepository.findByProductType(productType);
    }

    @Override
    public int calculateTotalQuantityByLot(SlaughterLot lot) {
        List<Product> products = getProductsByLot(lot);
        return products.size(); // sau poți folosi o sumă dacă ai cantități
    }

    @Override
    public List<Product> getProductsProducedBetween(LocalDate startDate, LocalDate endDate) {
        return productRepository.findByProductionDateBetween(startDate, endDate);
    }

    @Override
    public void markProductAsInspected(Long productId, boolean passed) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.markAsInspected(passed);
        productRepository.save(product);
    }
}
