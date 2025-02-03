package org.folio.mapper.configuration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import org.folio.dao.configuration.SettingEntity;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.Setting;
import org.springframework.stereotype.Component;

@Component
public class SettingMapper implements Function<SettingEntity, Setting> {

  @Override
  public Setting apply(SettingEntity entity) {
    return new Setting()
      .withKey(entity.getKey())
      .withDescription(entity.getDescription())
      .withValue(entity.getValue())
      .withType(Setting.Type.fromValue(entity.getType().value()))
      .withGroupId(entity.getGroupId())
      .withMetadata(new Metadata()
        .withCreatedByUserId(toString(entity.getCreatedByUserId()))
        .withUpdatedByUserId(toString(entity.getUpdatedByUserId()))
        .withCreatedDate(convertDate(entity.getCreatedDate()))
        .withUpdatedDate(convertDate(entity.getUpdatedDate()))
      );
  }

  private String toString(UUID uuid) {
    return uuid == null ? null : uuid.toString();
  }

  private Date convertDate(LocalDateTime localDateTime) {
    return localDateTime == null ? null : Date.from(localDateTime.toInstant(ZoneOffset.UTC));
  }

}
