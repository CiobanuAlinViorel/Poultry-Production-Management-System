package com.ppms.farm.services;
import com.ppms.farm.domain.Silo;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.repositories.SiloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class SiloService {
 private final SiloRepository repository; private final FarmService farms;
 @Transactional public SiloResponse create(UUID farmId,SiloRequest r){farms.require(farmId); if(repository.existsByFarmIdAndCodeIgnoreCase(farmId,r.code())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Silo code already exists"); Silo x=new Silo();x.setFarmId(farmId);x.setCode(r.code().trim());x.setName(r.name().trim());x.setCapacityKg(r.capacityKg());return CoreMapper.silo(repository.save(x));}
 public List<SiloResponse> list(UUID farmId){farms.require(farmId);return repository.findAllByFarmIdOrderByCode(farmId).stream().map(CoreMapper::silo).toList();}
}
