package org.folio.services.user.impl;

import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
import java.util.UUID;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.UserEventDao;
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
  private final ConfigurationService configurationService;
  private final UserEventDao userEventDao;

  public UserEventServiceImpl(Function<UserEvent, UserAuditEntity> eventToEntityMapper,
                               ConfigurationService configurationService,
                               UserEventDao userEventDao) {
    this.eventToEntityMapper = eventToEntityMapper;
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
    return save(event, tenantId);
  }

  private Future<String> save(UserEvent event, String tenantId) {
    var eventId = event.getId();
    LOGGER.debug("save:: Trying to save UserEvent with [tenantId: {}, eventId: {}, userId: {}]",
      tenantId, eventId, event.getUserId());

    var entity = eventToEntityMapper.apply(event);
    if (UserEventType.UPDATED.equals(event.getType()) && entity.diff() == null) {
      LOGGER.debug("save:: No diff calculated for UserEvent with [tenantId: {}, eventId: {}, userId: {}]",
        tenantId, eventId, event.getUserId());
      return Future.succeededFuture(eventId);
    }

    return userEventDao.save(entity, tenantId).map(eventId);
  }

  private Future<String> deleteAll(UserEvent event, String tenantId) {
    var userId = UUID.fromString(event.getUserId());
    LOGGER.debug("deleteAll:: Trying to delete all user audit records with [tenantId: {}, userId: {}]",
      tenantId, userId);
    return userEventDao.deleteByUserId(userId, tenantId)
      .map(event.getId());
  }
}
