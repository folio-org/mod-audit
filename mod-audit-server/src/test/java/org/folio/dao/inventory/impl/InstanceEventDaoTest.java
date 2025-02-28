package org.folio.dao.inventory.impl;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInventoryAuditEntity;
import static org.folio.utils.MockUtils.mockPostgresExecutionSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.folio.CopilotGenerated;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.MockUtils;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@CopilotGenerated(partiallyGenerated = true)
@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class InstanceEventDaoTest {

  @Mock
  PostgresClientFactory postgresClientFactory;
  @Mock
  PostgresClient postgresClient;
  @InjectMocks
  InstanceEventDao instanceEventDao;

  @BeforeEach
  void setUp() {
    lenient().when(postgresClientFactory.createInstance(TENANT_ID)).thenReturn(postgresClient);
    mockPostgresExecutionSuccess(2).when(postgresClient).execute(anyString(), any(Tuple.class), any());
  }

  @Test
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

  @Test
  void shouldGetInventoryAuditEntities(VertxTestContext ctx) {
    var entityId = UUID.randomUUID();
    var eventDate = Timestamp.from(Instant.now());
    var inventoryAuditEntity = createInventoryAuditEntity();

    doReturn(Future.succeededFuture(mockRowSet(inventoryAuditEntity)))
      .when(postgresClient).execute(anyString(), any(Tuple.class));

    instanceEventDao.get(entityId, eventDate, 10, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        assertEquals(1, result.size());
        assertEquals(inventoryAuditEntity.entityId(), result.get(0).entityId());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldDeleteAllInventoryAuditEntities(VertxTestContext ctx) {
    var entityId = UUID.randomUUID();

    doReturn(Future.succeededFuture())
      .when(postgresClient).execute(anyString(), any(Tuple.class));

    instanceEventDao.deleteAll(entityId, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(postgresClient, times(1)).execute(anyString(), any(Tuple.class));
        ctx.completeNow();
      }));
  }

  @Test
  void shouldDeleteOlderThanDate(VertxTestContext ctx) {
    var eventDate = Timestamp.from(Instant.now());

    doReturn(Future.succeededFuture())
      .when(postgresClient).execute(anyString(), any(Tuple.class));

    instanceEventDao.deleteOlderThanDate(eventDate, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(postgresClient, times(1)).execute(anyString(), any(Tuple.class));
        ctx.completeNow();
      }));
  }

  @Test
  void shouldReturnCorrectTableName() {
    var expectedTableName = "instance_audit";
    var actualTableName = instanceEventDao.tableName();
    assertEquals(expectedTableName, actualTableName);
  }

  private RowSet<Row> mockRowSet(InventoryAuditEntity entity) {
    var row = mock(Row.class);
    when(row.getUUID("event_id")).thenReturn(entity.eventId());
    when(row.getUUID("entity_id")).thenReturn(entity.entityId());
    when(row.getLocalDateTime("event_date")).thenReturn(entity.eventDate().toLocalDateTime());
    return MockUtils.mockRowSet(row);
  }
}
