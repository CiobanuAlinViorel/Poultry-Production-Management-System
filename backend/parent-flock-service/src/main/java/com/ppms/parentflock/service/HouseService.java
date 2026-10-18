package com.ppms.parentflock.service;

import com.ppms.parentflock.domain.House;
import com.ppms.parentflock.dtos.CreateHouseRequest;
import com.ppms.parentflock.dtos.HouseResponse;
import com.ppms.parentflock.repository.BreederSiteRepository;
import com.ppms.parentflock.repository.HouseRepository;
import com.ppms.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class HouseService {

    private final HouseRepository houseRepository;
    private final BreederSiteRepository breederSiteRepository;

    public HouseService(HouseRepository houseRepository, BreederSiteRepository breederSiteRepository) {
        this.houseRepository = houseRepository;
        this.breederSiteRepository = breederSiteRepository;
    }

    public HouseResponse create(UUID siteId, CreateHouseRequest request) {
        if (!breederSiteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("BreederSite", siteId);
        }
        House house = new House();
        house.setSiteId(siteId);
        house.setCode(request.code());
        house.setName(request.name());
        house.setCapacity(request.capacity());
        house.setAreaM2(request.areaM2());
        House saved = houseRepository.save(house);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<HouseResponse> listBySite(UUID siteId) {
        if (!breederSiteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("BreederSite", siteId);
        }
        return houseRepository.findBySiteId(siteId).stream()
                .map(this::toResponse)
                .toList();
    }

    private HouseResponse toResponse(House house) {
        return new HouseResponse(
                house.getId(),
                house.getSiteId(),
                house.getCode(),
                house.getName(),
                house.getCapacity(),
                house.getAreaM2(),
                house.isActive(),
                house.getCreatedAt(),
                house.getUpdatedAt()
        );
    }
}
