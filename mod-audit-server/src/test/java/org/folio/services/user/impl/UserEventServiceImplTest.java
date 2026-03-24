package org.folio.services.user.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.UserEventDao;
import org.folio.exception.ValidationException;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.mapper.user.UserEventToEntityMapper;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.model.UserAuditCollection;
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
  private Function<List<UserAuditEntity>, UserAuditCollection> entitiesToCollectionMapper;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private UserEventDao userEventDao;

  private UserEventService eventService;

  @BeforeEach
  void setUp() {
    eventService = new UserEventServiceImpl(eventToEntityMapper, entitiesToCollectionMapper, configurationService, userEventDao);
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
        verify(userEventDao).save(any(), anyString());
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
        verify(userEventDao).deleteByUserId(any(UUID.class), anyString());
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

  @Test
  void shouldRetrieveEventsSuccessfully(VertxTestContext ctx) {
    var userId = UUID.randomUUID().toString();
    var eventTs = "1672531200000";
    var userUUID = UUID.fromString(userId);
    var eventTsTimestamp = new Timestamp(Long.parseLong(eventTs));
    var userAuditCollection = new UserAuditCollection();

    when(userEventDao.count(userUUID, TENANT_ID)).thenReturn(Future.succeededFuture(1));
    when(configurationService.getSetting(
      org.folio.services.configuration.Setting.USER_RECORDS_PAGE_SIZE, TENANT_ID))
      .thenReturn(Future.succeededFuture(new Setting().withValue(10)));
    when(userEventDao.get(userUUID, eventTsTimestamp, 10, TENANT_ID))
      .thenReturn(Future.succeededFuture(List.of()));
    when(entitiesToCollectionMapper.apply(anyList())).thenReturn(userAuditCollection);

    eventService.getEvents(userId, eventTs, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(userEventDao).get(userUUID, eventTsTimestamp, 10, TENANT_ID);
        assertEquals(1, result.getTotalRecords());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldRetrieveEventsWithNullEventTs(VertxTestContext ctx) {
    var userId = UUID.randomUUID().toString();
    var userUUID = UUID.fromString(userId);
    var userAuditCollection = new UserAuditCollection();

    when(userEventDao.count(userUUID, TENANT_ID)).thenReturn(Future.succeededFuture(1));
    when(configurationService.getSetting(
      org.folio.services.configuration.Setting.USER_RECORDS_PAGE_SIZE, TENANT_ID))
      .thenReturn(Future.succeededFuture(new Setting().withValue(10)));
    when(userEventDao.get(userUUID, null, 10, TENANT_ID))
      .thenReturn(Future.succeededFuture(List.of()));
    when(entitiesToCollectionMapper.apply(anyList())).thenReturn(userAuditCollection);

    eventService.getEvents(userId, null, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(userEventDao).get(userUUID, null, 10, TENANT_ID);
        ctx.completeNow();
      }));
  }

  @Test
  void shouldRetrieveEmptyCollectionWhenCountIsZero(VertxTestContext ctx) {
    var userId = UUID.randomUUID().toString();
    var userUUID = UUID.fromString(userId);

    when(userEventDao.count(userUUID, TENANT_ID)).thenReturn(Future.succeededFuture(0));

    eventService.getEvents(userId, "1672531200000", TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        assertEquals(0, result.getTotalRecords());
        verify(userEventDao).count(userUUID, TENANT_ID);
        verifyNoMoreInteractions(userEventDao);
        verifyNoInteractions(configurationService);
        verifyNoInteractions(entitiesToCollectionMapper);
        ctx.completeNow();
      }));
  }

  @Test
  void shouldFailToRetrieveEventsWhenUserIdIsInvalid(VertxTestContext ctx) {
    eventService.getEvents("invalid-uuid", "1672531200000", TENANT_ID)
      .onComplete(ctx.failing(cause -> {
        assertInstanceOf(ValidationException.class, cause);
        verify(userEventDao, never()).get(any(UUID.class), any(Timestamp.class), anyInt(), anyString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldFailToRetrieveEventsWhenEventTsIsInvalid(VertxTestContext ctx) {
    var userId = UUID.randomUUID().toString();

    eventService.getEvents(userId, "invalid-date", TENANT_ID)
      .onComplete(ctx.failing(cause -> {
        assertInstanceOf(ValidationException.class, cause);
        verify(userEventDao, never()).get(any(UUID.class), any(Timestamp.class), anyInt(), anyString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldExpireRecordsSuccessfully(VertxTestContext ctx) {
    var tenDaysAgo = new Timestamp(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000);
    when(userEventDao.deleteOlderThanDate(tenDaysAgo, TENANT_ID)).thenReturn(Future.succeededFuture());

    eventService.expireRecords(TENANT_ID, tenDaysAgo)
      .onComplete(ctx.succeeding(result -> {
        verify(userEventDao).deleteOlderThanDate(tenDaysAgo, TENANT_ID);
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
