package com.ppms.farm.services;
import com.ppms.farm.domain.*;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.repositories.FarmDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class FarmDeliveryService {
 private final FarmDeliveryRepository repository; private final LotService lots;
 @Transactional public FarmDeliveryResponse record(UUID lotId,FarmDeliveryRequest r){Lot lot=lots.require(lotId);if(lot.getStatus()==Lot.Status.CLOSED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot deliver from a closed lot");if(r.dispatchDate().isBefore(lot.getArrivalDate()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Dispatch date precedes lot arrival");int placed=lot.getFemaleCount()+lot.getMaleCount();int delivered=repository.findAllByLotIdOrderByDispatchDateDesc(lotId).stream().mapToInt(FarmDelivery::getQuantity).sum();if(delivered+r.quantity()>placed)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Delivery quantity exceeds remaining birds");FarmDelivery x=new FarmDelivery();x.setLotId(lotId);x.setDispatchDate(r.dispatchDate());x.setQuantity(r.quantity());x.setAvgWeightKg(r.avgWeightKg());x.setFciRef(r.fciRef());return CoreMapper.delivery(repository.save(x));}
 public List<FarmDeliveryResponse> list(UUID lotId){lots.require(lotId);return repository.findAllByLotIdOrderByDispatchDateDesc(lotId).stream().map(CoreMapper::delivery).toList();}
}
