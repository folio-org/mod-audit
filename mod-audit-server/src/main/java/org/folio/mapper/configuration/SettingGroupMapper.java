package org.folio.mapper.configuration;

import java.util.function.Function;
import org.folio.dao.configuration.SettingGroupEntity;
import org.folio.rest.jaxrs.model.SettingGroup;
import org.springframework.stereotype.Component;

@Component
public class SettingGroupMapper implements Function<SettingGroupEntity, SettingGroup> {

  @Override
  public SettingGroup apply(SettingGroupEntity entity) {
    return new SettingGroup()
      .withId(entity.getId())
      .withName(entity.getName())
      .withDescription(entity.getDescription());
  }

}
