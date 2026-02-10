package org.folio.services.user.impl;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.UserEventDao;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.mapper.user.UserEventToEntityMapper;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.user.UserEventService;
import org.folio.util.user.UserEvent;
import org.folio.util.user.UserEventType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class UserEventServiceImplTest {

  private static final String TENANT_ID = "testTenant";

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private UserEventToEntityMapper eventToEntityMapper;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private UserEventDao userEventDao;

  private UserEventService eventService;

  @BeforeEach
  void setUp() {
    eventService = new UserEventServiceImpl(eventToEntityMapper, configurationService, userEventDao);
  }

  @Test
  void shouldSaveCreatedEventSuccessfully(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.CREATED);
    mockAuditEnabled(true);
    when(eventToEntityMapper.apply(event)).thenReturn(
      new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()),
        UUID.randomUUID(), UserEventType.CREATED.name(), null, null));
    when(userEventDao.save(any(), anyString())).thenReturn(Future.succeededFuture(rowSet));

    eventService.processEvent(event, TENANT_ID)
      .onComplete(ctx.succeeding(r -> {
        verify(userEventDao, times(1)).save(any(), anyString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldNotProcessWhenAuditDisabled(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.CREATED);
    mockAuditEnabled(false);

    eventService.processEvent(event, TENANT_ID)
      .onComplete(ctx.succeeding(r -> {
        verify(userEventDao, never()).save(any(), anyString());
        verify(userEventDao, never()).deleteByUserId(any(), anyString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldDeleteAllRecordsOnDeleteEvent(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.DELETED);
    mockAuditEnabled(true);
    when(userEventDao.deleteByUserId(any(UUID.class), anyString())).thenReturn(Future.succeededFuture());

    eventService.processEvent(event, TENANT_ID)
      .onComplete(ctx.succeeding(r -> {
        verify(userEventDao, times(1)).deleteByUserId(any(UUID.class), anyString());
        verify(userEventDao, never()).save(any(), anyString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldNotSaveUpdateEventWhenDiffIsNull(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.UPDATED);
    mockAuditEnabled(true);
    when(eventToEntityMapper.apply(event)).thenReturn(
      new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()),
        UUID.randomUUID(), UserEventType.UPDATED.name(), null, null));

    eventService.processEvent(event, TENANT_ID)
      .onComplete(ctx.succeeding(r -> {
        verify(userEventDao, never()).save(any(), anyString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldHandleDuplicateEvent(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.CREATED);
    mockAuditEnabled(true);
    when(eventToEntityMapper.apply(event)).thenReturn(
      new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()),
        UUID.randomUUID(), UserEventType.CREATED.name(), null, null));
    when(userEventDao.save(any(), anyString())).thenReturn(
      Future.failedFuture(new io.vertx.pgclient.PgException("duplicate", null, "23505", null)));

    eventService.processEvent(event, TENANT_ID)
      .onComplete(ctx.failing(cause -> {
        assertInstanceOf(DuplicateEventException.class, cause);
        ctx.completeNow();
      }));
  }

  private UserEvent createUserEvent(UserEventType type) {
    return UserEvent.builder()
      .id(UUID.randomUUID().toString())
      .type(type)
      .tenant(TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .userId(UUID.randomUUID().toString())
      .newValue(Map.of("key", "value"))
      .oldValue(Map.of("key", "oldValue"))
      .build();
  }

  private void mockAuditEnabled(boolean value) {
    when(configurationService.getSetting(any(), eq(TENANT_ID)))
      .thenReturn(Future.succeededFuture(new Setting().withValue(value)));
  }
}
