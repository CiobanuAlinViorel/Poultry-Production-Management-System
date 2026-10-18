package com.ppms.farm.services;
import com.ppms.farm.domain.*;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.repositories.LotRepository;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.settings.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class LotService {
 private final LotRepository repository; private final FarmService farms; private final HouseService houses; private final SettingsClient settings;
 @Transactional public LotResponse create(UUID farmId,LotRequest r){
  farms.require(farmId); House h=houses.require(r.houseId()); if(!h.getFarmId().equals(farmId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"House does not belong to farm");
  if(repository.existsByHouseIdAndStatus(r.houseId(),Lot.Status.ACTIVE)) throw new ResponseStatusException(HttpStatus.CONFLICT,"House already has an active lot");
  int birds=r.femaleCount()+r.maleCount(); if(birds<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Lot must contain at least one bird"); if(birds>h.getCapacity()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"House capacity exceeded");
  int minDowntime=Integer.parseInt(settings.get(SettingKeys.FA_DOWNTIME_MIN_DAYS,ScopeType.FARM,farmId,"10"));
  repository.findFirstByHouseIdAndStatusOrderByClosedAtDesc(r.houseId(),Lot.Status.CLOSED).ifPresent(last->{
   if(last.getClosedAt()!=null && last.getClosedAt().plus(minDowntime, ChronoUnit.DAYS).isAfter(r.arrivalDate().atStartOfDay()))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"House downtime minimum not met ("+minDowntime+" days)");
  });
  Lot x=new Lot();x.setFarmId(farmId);x.setHouseId(r.houseId());x.setBreedCode(r.breedCode().trim());x.setChickBatchId(r.chickBatchId());x.setArrivalDate(r.arrivalDate());x.setFemaleCount(r.femaleCount());x.setMaleCount(r.maleCount());x.setExpectedCatchDate(r.expectedCatchDate()); return CoreMapper.lot(repository.save(x));
 }
 public List<LotResponse> list(UUID farmId){farms.require(farmId);return repository.findAllByFarmIdOrderByArrivalDateDesc(farmId).stream().map(CoreMapper::lot).toList();}
 @Transactional public LotResponse close(UUID id){Lot x=require(id);if(x.getStatus()==Lot.Status.CLOSED) return CoreMapper.lot(x);x.setStatus(Lot.Status.CLOSED);x.setClosedAt(LocalDateTime.now());return CoreMapper.lot(repository.save(x));}
 public Lot require(UUID id){return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Lot not found"));}
}
