package org.folio.dao.inventory.impl;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInventoryAuditEntity;
import static org.folio.utils.MockUtils.mockPostgresExecutionSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Tuple;
import lombok.SneakyThrows;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class InstanceEventDaoTest {

  @Mock
  PostgresClientFactory postgresClientFactory;
  @Mock
  PostgresClient postgresClient;
  @InjectMocks
  InstanceEventDao instanceEventDao;

  @BeforeEach
  public void setUp() {
    when(postgresClientFactory.createInstance(TENANT_ID)).thenReturn(postgresClient);
    mockPostgresExecutionSuccess(2).when(postgresClient).execute(anyString(), any(Tuple.class), any());
  }

  @Test
  @SneakyThrows
  void shouldCreateEventProcessed(VertxTestContext ctx) {
    var inventoryAuditEvent = createInventoryAuditEntity();

    instanceEventDao.save(inventoryAuditEvent, TENANT_ID)
      .onComplete(ctx.succeeding(result -> ctx.completeNow()));

    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldHandleException(VertxTestContext ctx) {
    var inventoryAuditEvent = createInventoryAuditEntity();

    mockPostgresExecutionSuccess(2)
      .doThrow(new IllegalStateException("Error"))
      .when(postgresClient).execute(anyString(), any(Tuple.class), any());

    instanceEventDao.save(inventoryAuditEvent, TENANT_ID)
      .onComplete(ctx.succeeding(result ->
        instanceEventDao.save(inventoryAuditEvent, TENANT_ID)
          .onComplete(re -> {
            assertTrue(re.failed());
            assertInstanceOf(IllegalStateException.class, re.cause());
            assertEquals("Error", re.cause().getMessage());
            ctx.completeNow();
          })
      ));
    verify(postgresClientFactory, times(2)).createInstance(TENANT_ID);
  }
}