package com.ppms.farm.services;
import com.ppms.farm.domain.House;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.repositories.HouseRepository;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.settings.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class HouseService {
 private final HouseRepository repository; private final FarmService farms; private final SettingsClient settings;
 @Transactional public HouseResponse create(UUID farmId,HouseRequest r){
  farms.require(farmId); if(repository.existsByFarmIdAndCodeIgnoreCase(farmId,r.code())) throw new ResponseStatusException(HttpStatus.CONFLICT,"House code already exists");
  BigDecimal max=settings.get(SettingKeys.FA_STOCKING_MAX_KG_PER_M2, ScopeType.FARM,farmId,new BigDecimal("33"));
  if(r.areaM2().multiply(max).compareTo(BigDecimal.valueOf(r.capacity()))<0) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"House capacity exceeds configured stocking limit (assuming 1 kg minimum bird weight)");
  House x=new House(); x.setFarmId(farmId); x.setCode(r.code().trim()); x.setName(r.name().trim()); x.setCapacity(r.capacity()); x.setAreaM2(r.areaM2()); x.setActive(r.active()==null||r.active()); return CoreMapper.house(repository.save(x));
 }
 public List<HouseResponse> list(UUID farmId){farms.require(farmId); return repository.findAllByFarmIdOrderByCode(farmId).stream().map(CoreMapper::house).toList();}
 public House require(UUID id){return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"House not found"));}
}
