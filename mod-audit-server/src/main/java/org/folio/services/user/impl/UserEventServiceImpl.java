package org.folio.services.user.impl;

import static org.folio.dao.user.UserAuditConstants.ANONYMIZED_FIELD_PATHS;
import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.UserEventDao;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.UserAuditCollection;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.Setting;
import org.folio.services.user.UserEventService;
import org.folio.util.user.UserEvent;
import org.folio.util.user.UserEventType;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserEventServiceImpl implements UserEventService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final Function<UserEvent, UserAuditEntity> eventToEntityMapper;
  private final Function<List<UserAuditEntity>, UserAuditCollection> entitiesToCollectionMapper;
  private final ConfigurationService configurationService;
  private final UserEventDao userEventDao;

  public UserEventServiceImpl(Function<UserEvent, UserAuditEntity> eventToEntityMapper,
                               Function<List<UserAuditEntity>, UserAuditCollection> entitiesToCollectionMapper,
                               ConfigurationService configurationService,
                               UserEventDao userEventDao) {
    this.eventToEntityMapper = eventToEntityMapper;
    this.entitiesToCollectionMapper = entitiesToCollectionMapper;
    this.configurationService = configurationService;
    this.userEventDao = userEventDao;
  }

  @Override
  public Future<String> processEvent(UserEvent event, String tenantId) {
    LOGGER.debug("processEvent:: Trying to process UserEvent with [tenantId: {}, eventId: {}, userId: {}]",
      tenantId, event.getId(), event.getUserId());

    return configurationService.getSetting(Setting.USER_RECORDS_ENABLED, tenantId)
      .compose(setting -> {
        if (!Boolean.TRUE.equals(setting.getValue())) {
          LOGGER.debug("processEvent:: User audit is disabled for tenant [tenantId: {}]", tenantId);
          return Future.succeededFuture(event.getId());
        }
        return process(event, tenantId);
      })
      .recover(throwable -> {
        LOGGER.error("processEvent:: Could not process UserEvent for [tenantId: {}, eventId: {}, userId: {}]",
          tenantId, event.getId(), event.getUserId(), throwable);
        return handleFailures(throwable, event.getId());
      });
  }

  private Future<String> process(UserEvent event, String tenantId) {
    if (UserEventType.DELETED.equals(event.getType())) {
      return deleteAll(event, tenantId);
    }
    var entity = eventToEntityMapper.apply(event);
    var anonymizeSetting = configurationService.getSetting(Setting.USER_RECORDS_ANONYMIZE, tenantId);

    return Future.all(List.of(anonymizeSetting))
      .map(cf -> {
        var result = entity;
        if (Boolean.TRUE.equals(anonymizeSetting.result().getValue())) {
          result = anonymize(result);
        }
        return result;
      })
      .compose(transformed -> save(transformed, tenantId))
      .map(event.getId());
  }

  private Future<Void> save(UserAuditEntity entity, String tenantId) {
    if (isUpdateWithNoDiff(entity)) {
      LOGGER.debug("save:: No diff for UserAuditEntity [tenantId: {}, eventId: {}, userId: {}]",
        tenantId, entity.eventId(), entity.userId());
      return Future.succeededFuture();
    }
    LOGGER.debug("save:: Saving UserAuditEntity [tenantId: {}, eventId: {}, userId: {}]",
      tenantId, entity.eventId(), entity.userId());
    return userEventDao.save(entity, tenantId).mapEmpty();
  }

  private boolean isUpdateWithNoDiff(UserAuditEntity entity) {
    return UserEventType.UPDATED.name().equals(entity.action()) && entity.diff() == null;
  }

  private UserAuditEntity anonymize(UserAuditEntity entity) {
    return new UserAuditEntity(
      entity.eventId(),
      entity.eventDate(),
      entity.userId(),
      entity.action(),
      null,
      anonymizeDiff(entity.diff()));
  }

  private ChangeRecordDto anonymizeDiff(ChangeRecordDto diff) {
    if (diff == null) {
      return null;
    }
    var fieldChanges = diff.getFieldChanges();
    if (fieldChanges == null) {
      return (diff.getCollectionChanges() == null || diff.getCollectionChanges().isEmpty()) ? null : diff;
    }
    var remaining = fieldChanges.stream()
      .filter(fc -> !ANONYMIZED_FIELD_PATHS.contains(fc.getFullPath()))
      .toList();
    if (remaining.isEmpty() && (diff.getCollectionChanges() == null || diff.getCollectionChanges().isEmpty())) {
      return null;
    }
    return new ChangeRecordDto(remaining, diff.getCollectionChanges());
  }

  private Future<String> deleteAll(UserEvent event, String tenantId) {
    var userId = UUID.fromString(event.getUserId());
    LOGGER.debug("deleteAll:: Trying to delete all user audit records with [tenantId: {}, userId: {}]",
      tenantId, userId);
    return userEventDao.deleteByUserId(userId, tenantId)
      .map(event.getId());
  }

  @Override
  public Future<UserAuditCollection> getEvents(String userId, String eventTs, String tenantId) {
    LOGGER.debug("getEvents:: Trying to retrieve user events with [tenantId: {}, userId: {}, eventTs: {}]",
      tenantId, userId, eventTs);
    UUID userUUID;
    Timestamp eventTsTimestamp;
    try {
      userUUID = UUID.fromString(userId);
      eventTsTimestamp = eventTs == null ? null : new Timestamp(Long.parseLong(eventTs));
    } catch (IllegalArgumentException e) {
      LOGGER.error("getEvents:: Could not parse userId or eventTs [tenantId: {}, userId: {}, eventTs: {}]",
        tenantId, userId, eventTs, e);
      return Future.failedFuture(new ValidationException(e.getMessage()));
    }

    return userEventDao.count(userUUID, tenantId)
      .compose(count -> {
        if (count == 0) {
          LOGGER.debug("getEvents:: No user events found for [tenantId: {}, userId: {}, eventTs: {}]",
            tenantId, userId, eventTs);
          return Future.succeededFuture(new UserAuditCollection().withTotalRecords(0));
        }
        return configurationService.getSetting(Setting.USER_RECORDS_PAGE_SIZE, tenantId)
          .map(setting -> (Integer) setting.getValue())
          .compose(limit -> userEventDao.get(userUUID, eventTsTimestamp, limit, tenantId))
          .map(entitiesToCollectionMapper)
          .map(userAuditCollection -> userAuditCollection.withTotalRecords(count));
      });
  }
}
