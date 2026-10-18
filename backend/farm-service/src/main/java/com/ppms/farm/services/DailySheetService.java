package com.ppms.farm.services;
import com.ppms.farm.domain.*;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.repositories.DailySheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class DailySheetService {
 private final DailySheetRepository repository; private final LotService lots;
 @Transactional public DailySheetResponse record(UUID lotId,DailySheetRequest r){Lot lot=lots.require(lotId);if(lot.getStatus()==Lot.Status.CLOSED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot record a sheet for a closed lot");if(r.date().isBefore(lot.getArrivalDate()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sheet date precedes lot arrival");if(repository.existsByLotIdAndDate(lotId,r.date()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Daily sheet already exists");if(r.temperatureMin()!=null&&r.temperatureMax()!=null&&r.temperatureMin().compareTo(r.temperatureMax())>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Minimum temperature exceeds maximum");DailySheet x=new DailySheet();x.setLotId(lotId);x.setDate(r.date());x.setMortality(r.mortality());x.setFeedKg(r.feedKg());x.setWaterLiters(r.waterLiters());x.setAvgWeightG(r.avgWeightG());x.setTemperatureMin(r.temperatureMin());x.setTemperatureMax(r.temperatureMax());x.setHumidityPct(r.humidityPct());x.setComments(r.comments());return CoreMapper.sheet(repository.save(x));}
 public List<DailySheetResponse> list(UUID lotId){lots.require(lotId);return repository.findAllByLotIdOrderByDateDesc(lotId).stream().map(CoreMapper::sheet).toList();}
}
