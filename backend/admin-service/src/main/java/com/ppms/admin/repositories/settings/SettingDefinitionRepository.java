package com.ppms.admin.repositories.settings;

import com.ppms.admin.domain.settings.SettingDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingDefinitionRepository extends JpaRepository<SettingDefinition, UUID> {

    Optional<SettingDefinition> findByKey(String key);

    List<SettingDefinition> findByModule(String module);
}
