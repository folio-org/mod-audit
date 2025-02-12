package org.folio.services.marc.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.dao.marc.impl.MarcAuditDaoImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordType;
import org.folio.utils.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import io.vertx.core.Vertx;

public class MarcAuditServiceTest {

  @Mock
  private RowSet<Row> rowSet;

  private MarcAuditDao marcAuditDao;
  private MarcAuditServiceImpl marcAuditService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory = spy(new PostgresClientFactory(Vertx.vertx()));
      marcAuditDao = spy(new MarcAuditDaoImpl(postgresClientFactory));
      marcAuditService = new MarcAuditServiceImpl(marcAuditDao);
    }
  }

  @Test
  void shouldCallDaoForSuccessfulCase() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    doReturn(Future.succeededFuture(rowSet)).when(marcAuditDao).save(any(MarcAuditEntity.class), any(SourceRecordType.class), any());

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(marcAuditDao, times(1)).save(any(MarcAuditEntity.class), any(SourceRecordType.class), eq(EntityUtils.TENANT_ID));
  }

  @Test
  void shouldSkipSavingWhenNoChangesDetected() {
    var event = EntityUtils.sourceRecordDomainEventWithNoDiff();

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, never()).save(any(MarcAuditEntity.class), any(SourceRecordType.class), any());
  }

  @Test
  void shouldFailWhenDaoFails() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    doReturn(Future.failedFuture("Database error")).when(marcAuditDao).save(any(MarcAuditEntity.class), any(SourceRecordType.class), any());

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.failed());
      assertTrue(ar.cause().getMessage().contains("Database error"));
    });

    verify(marcAuditDao, times(1)).save(any(MarcAuditEntity.class), any(SourceRecordType.class), eq(EntityUtils.TENANT_ID));
  }
}
