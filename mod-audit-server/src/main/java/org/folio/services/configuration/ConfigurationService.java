package org.folio.services.configuration;

import static org.folio.util.ListUtils.mapItems;

import io.vertx.core.Future;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.folio.dao.configuration.SettingDao;
import org.folio.dao.configuration.SettingGroupDao;
import org.folio.mapper.configuration.SettingMappers;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.model.SettingCollection;
import org.folio.rest.jaxrs.model.SettingGroupCollection;
import org.folio.rest.persist.Conn;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

  private final SettingDao settingDao;
  private final SettingGroupDao settingGroupDao;
  private final SettingMappers settingMappers;
  private final SettingValidationService validationService;
  private final List<SettingChangeHandler> changeHandlers;
  private final PostgresClientFactory pgClientFactory;

  public Future<SettingGroupCollection> getAllSettingGroups(String tenantId) {
    return settingGroupDao.getAll(tenantId)
      .map(settingGroupEntities -> new SettingGroupCollection()
        .withTotalRecords(settingGroupEntities.size())
        .withSettingGroups(mapItems(settingGroupEntities, settingMappers.getSettingGroupMapper())));
  }

  public Future<SettingCollection> getAllSettingsByGroupId(String groupId, String tenantId) {
    return checkSettingGroup(groupId, tenantId)
      .compose(o -> settingDao.getAllByGroupId(groupId, tenantId))
      .map(settingEntities -> new SettingCollection()
        .withTotalRecords(settingEntities.size())
        .withSettings(mapItems(settingEntities, settingMappers.getSettingMapper())));
  }

  public Future<Void> updateSetting(String groupId, String settingKey, Setting setting, String userId,
                                    String tenantId) {
    validationService.validateSetting(setting, groupId, settingKey);
    var entity = settingMappers.getSettingEntityMapper().apply(setting);
    entity.setUpdatedDate(LocalDateTime.now(ZoneOffset.UTC));
    entity.setUpdatedByUserId(userId == null ? null : UUID.fromString(userId));
    var settingId = entity.getId();
    return pgClientFactory.createInstance(tenantId).withTrans(conn ->
      settingDao.exists(settingId, conn, tenantId)
        .compose(exists -> failSettingIfNotExist(groupId, settingKey, exists))
        .compose(ignored -> settingDao.getById(settingId, conn, tenantId))
        .compose(oldEntity -> {
          var oldValue = oldEntity.getValue();
          return settingDao.update(settingId, entity, conn, tenantId)
            .compose(ignored -> notifyHandlers(groupId, settingKey, oldValue, entity.getValue(), conn, tenantId));
        })
    );
  }

  public Future<Setting> getSetting(org.folio.services.configuration.Setting setting, String tenantId) {
    return settingDao.getById(setting.getSettingId(), tenantId)
      .map(settingMappers.getSettingMapper());
  }

  private Future<Void> checkSettingGroup(String groupId, String tenantId) {
    return settingGroupDao.exists(groupId, tenantId)
      .compose(exists -> failSettingGroupIfNotExist(groupId, exists));
  }

  private Future<Void> failSettingIfNotExist(String groupId, String settingKey, Boolean exists) {
    return failNotFound(exists, "Setting with key '%s' in group '%s' not found".formatted(settingKey, groupId));
  }

  private Future<Void> failSettingGroupIfNotExist(String groupId, Boolean exists) {
    return failNotFound(exists, "Setting group with id '%s' not found".formatted(groupId));
  }

  private Future<Void> failNotFound(boolean exists, String settingKey) {
    return exists
           ? Future.succeededFuture(null)
           : Future.failedFuture(new NotFoundException(settingKey));
  }

  private Future<Void> notifyHandlers(String groupId, String settingKey,
                                      Object oldValue, Object newValue, Conn conn, String tenantId) {
    if (Objects.equals(oldValue, newValue)) {
      return Future.succeededFuture();
    }
    return changeHandlers.stream()
      .reduce(
        Future.succeededFuture(),
        (chain, handler) -> chain.compose(ignored ->
          handler.onSettingChanged(groupId, settingKey, oldValue, newValue, conn, tenantId)),
        (a, b) -> a.compose(ignored -> b)
      );
  }
}
