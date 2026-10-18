package com.ppms.farm.services;
import com.ppms.farm.domain.*;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.repositories.DailySheetRepository;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.FarmDailySheetPayload;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class DailySheetService {
 private final DailySheetRepository repository; private final LotService lots; private final OutboxPublisher outboxPublisher;
 @Transactional public DailySheetResponse record(UUID lotId,DailySheetRequest r){Lot lot=lots.require(lotId);if(lot.getStatus()==Lot.Status.CLOSED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot record a sheet for a closed lot");if(r.date().isBefore(lot.getArrivalDate()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sheet date precedes lot arrival");if(repository.existsByLotIdAndDate(lotId,r.date()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Daily sheet already exists");if(r.temperatureMin()!=null&&r.temperatureMax()!=null&&r.temperatureMin().compareTo(r.temperatureMax())>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Minimum temperature exceeds maximum");DailySheet x=new DailySheet();x.setLotId(lotId);x.setDate(r.date());x.setMortality(r.mortality());x.setFeedKg(r.feedKg());x.setWaterLiters(r.waterLiters());x.setAvgWeightG(r.avgWeightG());x.setTemperatureMin(r.temperatureMin());x.setTemperatureMax(r.temperatureMax());x.setHumidityPct(r.humidityPct());x.setComments(r.comments());DailySheet saved=repository.save(x);
  outboxPublisher.publish(Topics.FARM_DAILY_OPS,new DomainEvent<>(UUID.randomUUID(),"farm.daily-sheet.recorded",1,"DailySheet",saved.getId(),lot.getFarmId(),null,
   new FarmDailySheetPayload(lotId,lot.getFarmId(),lot.getHouseId(),r.date(),r.mortality(),r.feedKg(),r.waterLiters(),r.avgWeightG(),r.temperatureMin(),r.temperatureMax(),r.humidityPct())));
  return CoreMapper.sheet(saved);}
 public List<DailySheetResponse> list(UUID lotId){lots.require(lotId);return repository.findAllByLotIdOrderByDateDesc(lotId).stream().map(CoreMapper::sheet).toList();}
}
