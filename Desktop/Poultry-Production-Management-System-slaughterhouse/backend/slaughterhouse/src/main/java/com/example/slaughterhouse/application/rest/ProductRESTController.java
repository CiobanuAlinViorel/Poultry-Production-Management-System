package com.example.slaughterhouse.application.rest;

import com.example.slaughterhouse.application.dto.ProductDto;
import com.example.slaughterhouse.application.transformation.ProductTransformationService;
import com.example.slaughterhouse.domain.entities.Product;
import com.example.slaughterhouse.domain.enums.ProductStatus;
import com.example.slaughterhouse.domain.enums.ProductType;
import com.example.slaughterhouse.infrastructure.persistance.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductRESTController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductTransformationService transformationService;

    @PostMapping
    public ResponseEntity<ProductDto> create(@RequestBody ProductDto productDto) {
        try {
            if (productDto.getSlaughterLotId() == null) {
                return ResponseEntity.badRequest().build();
            }

            Product product = transformationService.toEntity(productDto);
            if (product.getSlaughterLot() == null) {
                return ResponseEntity.badRequest().build();
            }

            product = productRepository.save(product);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(transformationService.toDto(product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transformationService.toDto(product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all products
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAll() {
        try {
            List<Product> products = productRepository.findByIsActiveTrue();
            List<ProductDto> dtos = products.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get products by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductDto>> getByStatus(@PathVariable ProductStatus status) {
        try {
            List<Product> products = productRepository.findByStatus(status);
            List<ProductDto> dtos = products.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get products by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ProductDto>> getByType(@PathVariable ProductType type) {
        try {
            List<Product> products = productRepository.findByProductType(type);
            List<ProductDto> dtos = products.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get products by production date range
     */
    @GetMapping("/production-date")
    public ResponseEntity<List<ProductDto>> getByProductionDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        try {
            List<Product> products = productRepository.findByProductionDateBetween(startDate, endDate);
            List<ProductDto> dtos = products.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get active products
     */
    @GetMapping("/active")
    public ResponseEntity<List<ProductDto>> getActive() {
        try {
            List<Product> products = productRepository.findByIsActiveTrue();
            List<ProductDto> dtos = products.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get inspected products
     */
    @GetMapping("/inspected")
    public ResponseEntity<List<ProductDto>> getInspected() {
        try {
            List<Product> products = productRepository.findByInspectionPassedTrue();
            List<ProductDto> dtos = products.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * UPDATE - Update product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody ProductDto productDto) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            // Update fields
            if (productDto.getProductType() != null) {
                product.setProductType(productDto.getProductType());
            }
            if (productDto.getCut() != null) {
                product.setCut(productDto.getCut());
            }
            if (productDto.getStatus() != null) {
                product.setStatus(productDto.getStatus());
            }
            if (productDto.getBatchNumber() != null) {
                product.setBatchNumber(productDto.getBatchNumber());
            }
            if (productDto.getInspectionPassed() != null) {
                product.setInspectionPassed(productDto.getInspectionPassed());
            }

            product = productRepository.save(product);
            return ResponseEntity.ok(transformationService.toDto(product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE - Delete product (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            product.setIsActive(false);
            productRepository.save(product);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BUSINESS LOGIC - Mark product as inspected
     */
    @PatchMapping("/{id}/inspect")
    public ResponseEntity<ProductDto> markAsInspected(
            @PathVariable Long id,
            @RequestParam Boolean passed) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            product.markAsInspected(passed);
            product = productRepository.save(product);
            return ResponseEntity.ok(transformationService.toDto(product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BUSINESS LOGIC - Package product
     */
    @PatchMapping("/{id}/package")
    public ResponseEntity<ProductDto> packageProduct(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            product.package_();
            product = productRepository.save(product);
            return ResponseEntity.ok(transformationService.toDto(product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * REFERENCE DATA - Get all available product types
     */
    @GetMapping("/types")
    public ResponseEntity<List<ProductTypeInfo>> getProductTypes() {
        try {
            List<ProductTypeInfo> types = java.util.Arrays.stream(ProductType.values())
                    .map(type -> new ProductTypeInfo(type.name(), type.getDisplayName()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Inner class for ProductType information
     */
    public static class ProductTypeInfo {
        private String code;
        private String displayName;

        public ProductTypeInfo(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}