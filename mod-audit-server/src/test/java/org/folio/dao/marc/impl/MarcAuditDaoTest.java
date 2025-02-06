package org.folio.dao.marc.impl;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordDomainEventType;
import org.folio.util.marc.SourceRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MarcAuditDaoTest {

  private static final String TENANT_ID = "diku";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  private MarcAuditDao marcAuditDao = new MarcAuditDaoImpl(postgresClientFactory);

  private MarcAuditEntity entity;

  @BeforeEach
  void setUp() {
    entity = new MarcAuditEntity(UUID.randomUUID().toString(), LocalDateTime.now(), UUID.randomUUID().toString(), "origin", SourceRecordDomainEventType.SOURCE_RECORD_CREATED.getValue(), UUID.randomUUID().toString(), SourceRecordType.MARC_BIB, Map.of());
    MockitoAnnotations.openMocks(this);
    marcAuditDao = new MarcAuditDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var saveFuture = marcAuditDao.save(entity, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldThrowConstraintViolation() {
    var saveFuture = marcAuditDao.save(entity, TENANT_ID);
    saveFuture.onComplete(ar -> {
      var reSaveFuture = marcAuditDao.save(entity, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertInstanceOf(PgException.class, re.cause());
      });
    });

    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }
}
