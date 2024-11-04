package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class OrderEventsDaoTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  OrderEventsDaoImpl orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var orderAuditEvent = createOrderAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
      verify(postgresClientFactory, times(1)).createInstance(TENANT_ID).execute(anyString(), any(Tuple.class));
    });
  }

  @Test
  void shouldThrowConstraintViolation() {
    var orderAuditEvent = createOrderAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_order_log_pkey\" (23505)", re.cause().getMessage());
        verify(postgresClientFactory, times(1)).createInstance(TENANT_ID).execute(anyString(), any(Tuple.class));
      });
    });
  }

  @Test
  void shouldGetCreatedEvent() {
    String id = UUID.randomUUID().toString();
    var orderAuditEvent = createOrderAuditEvent(id);

    orderEventDao.save(orderAuditEvent, TENANT_ID);

    Future<OrderAuditEventCollection> dto = orderEventDao.getAuditEventsByOrderId(id, "action_date", "desc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      OrderAuditEventCollection orderAuditEventOptional = ar.result();
      List<OrderAuditEvent> orderAuditEventList = orderAuditEventOptional.getOrderAuditEvents();

      assertEquals(orderAuditEventList.get(0).getId(), id);
      assertEquals(OrderAuditEvent.Action.CREATE.value(), orderAuditEventList.get(0).getAction().value());
      verify(postgresClientFactory, times(1)).createInstance(TENANT_ID).selectRead(anyString(), any(Tuple.class), any());
    });
  }
}


