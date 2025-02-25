package org.folio.services.marc.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.dao.marc.impl.MarcAuditDaoImpl;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.MarcAuditCollection;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.services.configuration.ConfigurationService;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordType;
import org.folio.utils.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MarcAuditServiceTest {
  private static final String DATE_TIME = String.valueOf(Instant.now().toEpochMilli());
  private static final String INVALID_ENTITY_ID = "invalid-uuid";

  @Mock
  private RowSet<Row> rowSet;

  private MarcAuditDao marcAuditDao;
  private MarcAuditServiceImpl marcAuditService;
  private ConfigurationService configurationService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory = spy(new PostgresClientFactory(Vertx.vertx()));
      marcAuditDao = spy(new MarcAuditDaoImpl(postgresClientFactory));
      configurationService = mock(ConfigurationService.class);
      marcAuditService = new MarcAuditServiceImpl(marcAuditDao, configurationService);
    }
  }

  @Test
  void shouldSaveEventWhenAuditIsEnabledAndDaoCompletes() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    mockAuditEnabled(true);
    doReturn(Future.succeededFuture(rowSet)).when(marcAuditDao).save(any(MarcAuditEntity.class), any(SourceRecordType.class), anyString());

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(marcAuditDao, times(1)).save(any(MarcAuditEntity.class), eq(event.getRecordType()), eq(EntityUtils.TENANT_ID));
  }

  @Test
  void shouldNotSaveEventWhenNoChangesDetected() {
    var event = EntityUtils.sourceRecordDomainEventWithNoDiff();

    mockAuditEnabled(true);

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, never()).save(any(MarcAuditEntity.class), eq(event.getRecordType()), eq(EntityUtils.TENANT_ID));
  }

  @Test
  void shouldNotSaveEventWhenAuditFeatureIsDisabled() {
    var event = EntityUtils.sourceRecordDomainEventWithNoDiff();

    mockAuditEnabled(false);

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, never()).save(any(MarcAuditEntity.class), eq(event.getRecordType()), eq(EntityUtils.TENANT_ID));
  }

  @Test
  void shouldFailToSaveEventWhenDaoThrowsError() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    mockAuditEnabled(true);
    doReturn(Future.failedFuture("Database error")).when(marcAuditDao).save(any(MarcAuditEntity.class), any(SourceRecordType.class),any());

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.failed());
      assertTrue(ar.cause().getMessage().contains("Database error"));
    });

    verify(marcAuditDao, times(1)).save(any(MarcAuditEntity.class), any(SourceRecordType.class), eq(TENANT_ID));
  }

  @Test
  void shouldRetrieveMarcBibAuditRecordsWhenDaoSucceeds() {
    when(configurationService.getSetting(any(), eq(EntityUtils.TENANT_ID)))
      .thenReturn(Future.succeededFuture(new Setting().withValue(10)));
    when(marcAuditDao.count(any(UUID.class), any(SourceRecordType.class), anyString()))
      .thenReturn(Future.succeededFuture(1));
    var saveFuture = marcAuditService.getMarcAuditRecords(EntityUtils.SOURCE_RECORD_ID, SourceRecordType.MARC_BIB, TENANT_ID, DATE_TIME);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, times(1)).get(any(UUID.class), any(SourceRecordType.class), eq(TENANT_ID), any(LocalDateTime.class), anyInt());
  }

  @Test
  void shouldRetrieveMarcAuthorityAuditRecordsWhenDaoSucceeds() {
    when(configurationService.getSetting(org.folio.services.configuration.Setting.AUTHORITY_RECORDS_PAGE_SIZE, TENANT_ID))
      .thenReturn(Future.succeededFuture(new org.folio.rest.jaxrs.model.Setting().withValue(10)));
    when(marcAuditDao.count(any(UUID.class), any(SourceRecordType.class), anyString()))
      .thenReturn(Future.succeededFuture(1));
    var saveFuture = marcAuditService.getMarcAuditRecords(EntityUtils.SOURCE_RECORD_ID, SourceRecordType.MARC_AUTHORITY, TENANT_ID, DATE_TIME);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, times(1)).get(any(UUID.class), any(SourceRecordType.class), eq(TENANT_ID), any(LocalDateTime.class), anyInt());
  }

  @Test
  void shouldNotRetrieveRecordsWhenCountIsZero() {
    when(configurationService.getSetting(any(), eq(EntityUtils.TENANT_ID)))
      .thenReturn(Future.succeededFuture(new Setting().withValue(10)));
    when(marcAuditDao.count(any(UUID.class), any(SourceRecordType.class), anyString()))
      .thenReturn(Future.succeededFuture(0));
    var saveFuture = marcAuditService.getMarcAuditRecords(EntityUtils.SOURCE_RECORD_ID, SourceRecordType.MARC_BIB, TENANT_ID, DATE_TIME);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, times(0)).get(any(UUID.class), any(SourceRecordType.class), eq(TENANT_ID), any(LocalDateTime.class), anyInt());
  }

  @Test
  void shouldFailToRetrieveRecordsWhenInvalidEntityIdProvided() {
    Future<MarcAuditCollection> result = marcAuditService.getMarcAuditRecords(INVALID_ENTITY_ID, SourceRecordType.MARC_BIB, TENANT_ID, DATE_TIME);

    assertTrue(result.failed());
    assertInstanceOf(ValidationException.class, result.cause());
    verify(marcAuditDao, never()).get(any(), any(), any(), any(), anyInt());
  }

  @ParameterizedTest
  @EnumSource(SourceRecordType.class)
  void shouldExpireRecordsSuccessfully(SourceRecordType recordType) {
    var expireOlderThan = Timestamp.from(Instant.now().minusSeconds(3600));

    doReturn(Future.succeededFuture()).when(marcAuditDao).deleteOlderThanDate(expireOlderThan, TENANT_ID, recordType);

    var expireFuture = marcAuditService.expireRecords(TENANT_ID, expireOlderThan, recordType);
    expireFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(marcAuditDao).deleteOlderThanDate(expireOlderThan, TENANT_ID, recordType);
  }

  private void mockAuditEnabled(boolean value) {
    when(configurationService.getSetting(any(), eq(EntityUtils.TENANT_ID)))
      .thenReturn(Future.succeededFuture(new Setting().withValue(value)));
  }
}
