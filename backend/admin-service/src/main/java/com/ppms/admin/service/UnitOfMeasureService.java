package com.ppms.admin.service;

import com.ppms.admin.api.catalog.UnitOfMeasureDto;
import com.ppms.admin.api.catalog.UnitOfMeasureRequest;
import com.ppms.admin.domain.catalog.UnitOfMeasure;
import com.ppms.admin.repositories.catalog.UnitOfMeasureRepository;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.exception.ValidationException;
import com.ppms.shared.web.PageRequestParams;
import com.ppms.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnitOfMeasureService {

    private final UnitOfMeasureRepository unitRepository;

    private static final Set<String> ALLOWED_SORT = Set.of("code", "name", "kind", "active");

    @Transactional(readOnly = true)
    public PageResponse<UnitOfMeasureDto> list(int page, int size, String sort, boolean activeOnly) {
        Pageable pageable = PageRequestParams.of(page, size, sort, ALLOWED_SORT).toPageable();
        Page<UnitOfMeasure> result = activeOnly ? unitRepository.findByActiveTrue(pageable) : unitRepository.findAll(pageable);
        return new PageResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                sort
        );
    }

    @Transactional(readOnly = true)
    public UnitOfMeasureDto get(UUID id) {
        return toDto(unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UnitOfMeasure", id)));
    }

    @Transactional
    public UnitOfMeasureDto create(UnitOfMeasureRequest request) {
        unitRepository.findByCode(request.code()).ifPresent(u -> {
            throw new ValidationException("Unit code already exists: " + request.code());
        });
        UnitOfMeasure unit = new UnitOfMeasure();
        updateEntity(unit, request);
        return toDto(unitRepository.save(unit));
    }

    @Transactional
    public UnitOfMeasureDto update(UUID id, UnitOfMeasureRequest request) {
        UnitOfMeasure unit = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UnitOfMeasure", id));
        unitRepository.findByCode(request.code()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ValidationException("Unit code already exists: " + request.code());
            }
        });
        updateEntity(unit, request);
        return toDto(unitRepository.save(unit));
    }

    @Transactional
    public void delete(UUID id) {
        UnitOfMeasure unit = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UnitOfMeasure", id));
        unit.setActive(false);
    }

    private void updateEntity(UnitOfMeasure unit, UnitOfMeasureRequest request) {
        unit.setCode(request.code());
        unit.setName(request.name());
        unit.setKind(request.kind());
        unit.setActive(request.active());
    }

    private UnitOfMeasureDto toDto(UnitOfMeasure unit) {
        return new UnitOfMeasureDto(unit.getId(), unit.getCode(), unit.getName(), unit.getKind(), unit.isActive());
    }
}
