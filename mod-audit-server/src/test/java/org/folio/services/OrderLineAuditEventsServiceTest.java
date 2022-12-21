package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.OrderLineEventsDao;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;
import org.folio.services.acquisition.impl.OrderLineAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderLineAuditEventsServiceTest {

  private static final String TENANT_ID = "diku";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  OrderLineEventsDao orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  OrderLineAuditEventsServiceImpl orderLineAuditEventService = new OrderLineAuditEventsServiceImpl(orderLineEventsDao);

  @Test
  public void shouldCallDaoForSuccessfulCase() {
    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderLineSnapshot("{\"name\":\"New OrderLine Product\"}");

    Future<RowSet<Row>> saveFuture = orderLineAuditEventService.saveOrderLineAuditEvent(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldGetOrderLineDto() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product");

    String id = UUID.randomUUID().toString();
    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(id)
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withOrderLineId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderLineSnapshot(jsonObject);

    orderLineAuditEventService.saveOrderLineAuditEvent(orderLineAuditEvent, TENANT_ID);

    Future<OrderLineAuditEventCollection> dto = orderLineAuditEventService.getAuditEventsByOrderLineId(id, 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      OrderLineAuditEventCollection orderLineAuditEventOptional = ar.result();
      List<OrderLineAuditEvent> orderLineAuditEventList = orderLineAuditEventOptional.getOrderLineAuditEvents();

      assertEquals(orderLineAuditEventList.get(0).getId(), id);
      assertEquals(OrderLineAuditEvent.Action.CREATE, orderLineAuditEventList.get(0).getAction());

    });
  }

}
