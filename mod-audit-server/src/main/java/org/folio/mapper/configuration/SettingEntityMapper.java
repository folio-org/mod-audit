package org.folio.mapper.configuration;

import java.util.function.Function;
import org.folio.dao.configuration.SettingEntity;
import org.folio.dao.configuration.SettingValueType;
import org.folio.rest.jaxrs.model.Setting;
import org.springframework.stereotype.Component;

@Component
public class SettingEntityMapper implements Function<Setting, SettingEntity> {

  @Override
  public SettingEntity apply(Setting source) {
    return SettingEntity.builder()
      .id(constructSettingId(source.getGroupId(), source.getKey()))
      .key(source.getKey())
      .value(source.getValue())
      .type(SettingValueType.fromValue(source.getType().value()))
      .groupId(source.getGroupId())
      .description(source.getDescription())
      .build();
  }

  private String constructSettingId(String groupId, String settingKey) {
    return groupId + "." + settingKey;
  }

}
