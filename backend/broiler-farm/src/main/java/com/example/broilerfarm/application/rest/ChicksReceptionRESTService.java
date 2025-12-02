package com.example.broilerfarm.application.rest;

import com.example.broilerfarm.application.IChickReceptionApplicationService;
import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.services.ChicksReceptionTransformationService;
import com.example.broilerfarm.domain.entities.ChicksReception;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksReceptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chicks-receptions")
@CrossOrigin(origins = "*")
public class ChicksReceptionRESTService {

    @Autowired
    private ChicksReceptionRepository chicksReceptionRepository;

    @Autowired
    private ChicksReceptionTransformationService transformationService;

    /**
     * CREATE
     */
    @PostMapping
    public ResponseEntity<ChickReceptionDto> create(@RequestBody ChickReceptionDto receptionDto) {
        try {
            if (receptionDto.getFarmId() == null || receptionDto.getEmployeeid() == null) {
                return ResponseEntity.badRequest().build();
            }

            ChicksReception reception = transformationService.toEntity(receptionDto);
            if (reception.getFarm() == null || reception.getReceivingEmployee() == null) {
                return ResponseEntity.badRequest().build();
            }

            reception = chicksReceptionRepository.save(reception);
            return ResponseEntity.status(HttpStatus.CREATED).body(transformationService.toDto(reception));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChickReceptionDto> getById(@PathVariable Long id) {
        try {
            ChicksReception reception = chicksReceptionRepository.findById(id).orElse(null);
            if (reception == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transformationService.toDto(reception));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all
     */
    @GetMapping
    public ResponseEntity<List<ChickReceptionDto>> getAll() {
        try {
            List<ChicksReception> receptions = chicksReceptionRepository.findAll();
            List<ChickReceptionDto> dtos = receptions.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get by Farm ID
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<ChickReceptionDto>> getByFarmId(@PathVariable Long farmId) {
        try {
            List<ChicksReception> receptions = chicksReceptionRepository.findByFarmId(farmId);
            List<ChickReceptionDto> dtos = receptions.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * UPDATE
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChickReceptionDto> update(@PathVariable Long id, @RequestBody ChickReceptionDto receptionDto) {
        try {
            ChicksReception reception = chicksReceptionRepository.findById(id).orElse(null);
            if (reception == null) {
                return ResponseEntity.notFound().build();
            }

            if (receptionDto.getReceptionDate() != null) {
                reception.setReceptionDate(receptionDto.getReceptionDate());
            }
            if (receptionDto.getTransportConditions() != null) {
                reception.setTransportConditions(receptionDto.getTransportConditions());
            }
            if (receptionDto.getTruckInfo() != null) {
                reception.setTruckInfo(receptionDto.getTruckInfo());
            }
            if (receptionDto.getReferenceDocument() != null) {
                reception.setDocumentReference(receptionDto.getReferenceDocument());
            }
            if (receptionDto.getReceptionStatus() != null) {
                reception.setStatus(receptionDto.getReceptionStatus());
            }

            reception = chicksReceptionRepository.save(reception);
            return ResponseEntity.ok(transformationService.toDto(reception));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            ChicksReception reception = chicksReceptionRepository.findById(id).orElse(null);
            if (reception == null) {
                return ResponseEntity.notFound().build();
            }

            chicksReceptionRepository.delete(reception);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}