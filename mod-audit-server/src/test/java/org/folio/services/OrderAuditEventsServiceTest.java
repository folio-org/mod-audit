package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.OrderEventsDao;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.folio.services.acquisition.impl.OrderAuditEventsServiceImpl;
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

public class OrderAuditEventsServiceTest {

  private static final String TENANT_ID = "diku";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  OrderEventsDao orderEventsDao = new OrderEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  OrderAuditEventsServiceImpl orderAuditEventService = new OrderAuditEventsServiceImpl(orderEventsDao);

  @Test
  void shouldCallDaoForSuccessfulCase() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product");

    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);

    Future<RowSet<Row>> saveFuture = orderAuditEventService.saveOrderAuditEvent(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldGetDto() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product");

    String id = UUID.randomUUID().toString();
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(id)
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);

    orderAuditEventService.saveOrderAuditEvent(orderAuditEvent, TENANT_ID);

    Future<OrderAuditEventCollection> dto = orderAuditEventService.getAuditEventsByOrderId(id,1,1, TENANT_ID);
    dto.onComplete(ar -> {
      OrderAuditEventCollection orderAuditEventOptional = ar.result();
      List<OrderAuditEvent> orderAuditEventList = orderAuditEventOptional.getOrderAuditEvents();

      assertEquals(orderAuditEventList.get(0).getId(), id);
      assertEquals(OrderAuditEvent.Action.CREATE.value(), orderAuditEventList.get(0).getAction().value());

    });
  }

}
