package org.folio.dao.inventory.impl;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInventoryAuditEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import org.folio.CopilotGenerated;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@CopilotGenerated
class InstanceEventDaoTest {

  @Spy
  PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  InstanceEventDao instanceEventDao;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    instanceEventDao = new InstanceEventDao(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var inventoryAuditEvent = createInventoryAuditEntity();

    var saveFuture = instanceEventDao.save(inventoryAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldThrowConstrainViolation() {
    var inventoryAuditEvent = createInventoryAuditEntity();

    var saveFuture = instanceEventDao.save(inventoryAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      var reSaveFuture = instanceEventDao.save(inventoryAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"inventory_audit_pkey\" (23505)",
          re.cause().getMessage());
      });
    });
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }
}