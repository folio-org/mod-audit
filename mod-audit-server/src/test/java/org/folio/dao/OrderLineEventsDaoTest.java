package org.folio.dao;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderLineEventsDaoTest {

  private static final String TENANT_ID = "diku";

  public static final String ORDER_LINE_ID = "a21fc51c-d46b-439b-8c79-9b2be41b79a6";

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
    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withOrderLineId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date());
    Future<RowSet<Row>> saveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
        assertTrue(ar.succeeded());
      });
  }

  @Test
  void shouldThrowConstraintViolation() {
    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withOrderLineId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date());

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
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product2");
    String id = UUID.randomUUID().toString();

    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(id)
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withOrderLineId(ORDER_LINE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderLineSnapshot(jsonObject);

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


