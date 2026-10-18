package com.ppms.admin.service;

import com.ppms.admin.api.catalog.BreedDto;
import com.ppms.admin.api.catalog.BreedRequest;
import com.ppms.admin.domain.catalog.Breed;
import com.ppms.admin.repositories.catalog.BreedRepository;
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
public class BreedService {

    private final BreedRepository breedRepository;

    private static final Set<String> ALLOWED_SORT = Set.of("code", "name", "kind", "active");

    @Transactional(readOnly = true)
    public PageResponse<BreedDto> list(int page, int size, String sort, boolean activeOnly) {
        Pageable pageable = PageRequestParams.of(page, size, sort, ALLOWED_SORT).toPageable();
        Page<Breed> result = activeOnly ? breedRepository.findByActiveTrue(pageable) : breedRepository.findAll(pageable);
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
    public BreedDto get(UUID id) {
        return toDto(breedRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Breed", id)));
    }

    @Transactional
    public BreedDto create(BreedRequest request) {
        breedRepository.findByCode(request.code()).ifPresent(b -> {
            throw new ValidationException("Breed code already exists: " + request.code());
        });
        Breed breed = new Breed();
        updateEntity(breed, request);
        return toDto(breedRepository.save(breed));
    }

    @Transactional
    public BreedDto update(UUID id, BreedRequest request) {
        Breed breed = breedRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Breed", id));
        breedRepository.findByCode(request.code()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ValidationException("Breed code already exists: " + request.code());
            }
        });
        updateEntity(breed, request);
        return toDto(breedRepository.save(breed));
    }

    @Transactional
    public void delete(UUID id) {
        Breed breed = breedRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Breed", id));
        breed.setActive(false);
    }

    private void updateEntity(Breed breed, BreedRequest request) {
        breed.setCode(request.code());
        breed.setName(request.name());
        breed.setKind(request.kind());
        breed.setActive(request.active());
    }

    private BreedDto toDto(Breed breed) {
        return new BreedDto(breed.getId(), breed.getCode(), breed.getName(), breed.getKind(), breed.isActive());
    }
}
