package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderLineAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class OrderLineEventsDaoTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  OrderLineEventsDaoImpl orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var orderLineAuditEvent = createOrderLineAuditEvent(UUID.randomUUID().toString());
    Future<RowSet<Row>> saveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));
  }

  @Test
  void shouldThrowConstraintViolation() {
    var orderLineAuditEvent = createOrderLineAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof  PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_order_line_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
  }

  @Test
  void shouldGetCreatedEvent() {
    String id = UUID.randomUUID().toString();
    var orderLineAuditEvent = createOrderLineAuditEvent(id);

    orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);

    Future<OrderLineAuditEventCollection> dto = orderLineEventsDao.getAuditEventsByOrderLineId(id, "action_date",  "asc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      OrderLineAuditEventCollection orderLineAuditEventOptional = ar.result();
      List<OrderLineAuditEvent> orderLineAuditEventList = orderLineAuditEventOptional.getOrderLineAuditEvents();

      assertEquals(orderLineAuditEventList.get(0).getId(), id);
      assertEquals(OrderAuditEvent.Action.CREATE.value(), orderLineAuditEventList.get(0).getAction().value());
    });
  }

}


