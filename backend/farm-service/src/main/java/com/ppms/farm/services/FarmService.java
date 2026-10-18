package com.ppms.farm.services;
import com.ppms.farm.domain.Farm;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.repositories.FarmRepository;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.SiteDirectoryPayload;
import com.ppms.shared.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class FarmService {
 private final FarmRepository repository;
 private final OutboxPublisher outboxPublisher;
 @Transactional public FarmResponse create(FarmRequest r){
  if(repository.existsByCodeIgnoreCase(r.code())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Farm code already exists");
  Farm x=new Farm(); x.setCode(r.code().trim()); x.setName(r.name().trim()); x.setAddress(r.address()); x.setCounty(r.county()); x.setActive(r.active()==null||r.active());
  Farm saved = repository.save(x);
  outboxPublisher.publish(Topics.SITE_DIRECTORY, new DomainEvent<>(UUID.randomUUID(), "site.directory.upserted", 1, "SiteDirectory", saved.getId(), saved.getId(), null,
          new SiteDirectoryPayload(saved.getId(), "FARM", saved.getCode(), saved.getName(), saved.getAddress(), saved.getCounty(), saved.isActive(), "farm-service", Instant.now())));
  return CoreMapper.farm(saved);
 }
 public FarmResponse get(UUID id){return CoreMapper.farm(require(id));}
 public List<FarmResponse> list(){return repository.findAll().stream().map(CoreMapper::farm).toList();}
 public Farm require(UUID id){return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Farm not found"));}
}
