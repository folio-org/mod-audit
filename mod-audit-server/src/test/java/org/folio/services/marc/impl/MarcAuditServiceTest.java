package org.folio.services.marc.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.dao.marc.impl.MarcAuditDaoImpl;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.services.configuration.ConfigurationService;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MarcAuditServiceTest {

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
  void shouldCallDaoForSuccessfulCase() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    mockAuditEnabled(true);
    doReturn(Future.succeededFuture(rowSet)).when(marcAuditDao).save(any(MarcAuditEntity.class), any());

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(marcAuditDao, times(1)).save(any(MarcAuditEntity.class), eq(EntityUtils.TENANT_ID));
  }

  @Test
  void shouldSkipSavingWhenNoChangesDetected() {
    var event = EntityUtils.sourceRecordDomainEventWithNoDiff();

    mockAuditEnabled(true);

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, never()).save(any(MarcAuditEntity.class), any());
  }

  @Test
  void shouldSkipSavingWhenFeatureIsDisabled() {
    var event = EntityUtils.sourceRecordDomainEventWithNoDiff();

    mockAuditEnabled(false);

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(marcAuditDao, never()).save(any(MarcAuditEntity.class), any());
  }

  @Test
  void shouldFailWhenDaoFails() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    mockAuditEnabled(true);
    doReturn(Future.failedFuture("Database error")).when(marcAuditDao).save(any(MarcAuditEntity.class), any());

    var saveFuture = marcAuditService.saveMarcDomainEvent(event);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.failed());
      assertTrue(ar.cause().getMessage().contains("Database error"));
    });

    verify(marcAuditDao, times(1)).save(any(MarcAuditEntity.class), eq(EntityUtils.TENANT_ID));
  }

  private void mockAuditEnabled(boolean value) {
    when(configurationService.getSetting(any(), eq(EntityUtils.TENANT_ID)))
      .thenReturn(Future.succeededFuture(new Setting().withValue(value)));
  }
}
