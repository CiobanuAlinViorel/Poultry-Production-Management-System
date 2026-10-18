package com.ppms.admin.service;
import com.ppms.admin.api.workforce.WorkforceDtos.*;
import com.ppms.admin.domain.workforce.Position;
import com.ppms.admin.repositories.workforce.PositionRepository;
import com.ppms.shared.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class PositionService {
 private final PositionRepository repository;
 @Transactional(readOnly=true) public List<PositionDto> list(){return repository.findAll().stream().map(this::dto).toList();}
 @Transactional public PositionDto create(PositionRequest r){if(repository.findByCode(r.code()).isPresent())throw new ValidationException("Position code already exists: "+r.code()); return dto(repository.save(apply(new Position(),r)));}
 @Transactional public PositionDto update(UUID id,PositionRequest r){Position p=repository.findById(id).orElseThrow(()->new EntityNotFoundException("Position",id)); repository.findByCode(r.code()).filter(x->!x.getId().equals(id)).ifPresent(x->{throw new ValidationException("Position code already exists: "+r.code());}); return dto(repository.save(apply(p,r)));}
 private Position apply(Position p,PositionRequest r){p.setCode(r.code());p.setNameRo(r.nameRo());p.setNameEn(r.nameEn());p.setModule(r.module());p.setDefaultRoleIds(r.defaultRoleIds()==null?new UUID[0]:r.defaultRoleIds());return p;}
 private PositionDto dto(Position p){return new PositionDto(p.getId(),p.getCode(),p.getNameRo(),p.getNameEn(),p.getModule(),p.getDefaultRoleIds());}
}
