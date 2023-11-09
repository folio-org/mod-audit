package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
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

import java.util.List;
import java.util.UUID;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderLineAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderLineAuditEventsServiceTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  OrderLineEventsDao orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);
  @InjectMocks
  OrderLineAuditEventsServiceImpl orderLineAuditEventService = new OrderLineAuditEventsServiceImpl(orderLineEventsDao);

  @Test
  public void shouldCallDaoForSuccessfulCase() {
    var orderLineAuditEvent = createOrderLineAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = orderLineAuditEventService.saveOrderLineAuditEvent(orderLineAuditEvent, TENANT_ID);

    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldGetOrderLineDto() {
    String id = UUID.randomUUID().toString();
    var orderLineAuditEvent = createOrderLineAuditEvent(id);

    orderLineAuditEventService.saveOrderLineAuditEvent(orderLineAuditEvent, TENANT_ID);

    Future<OrderLineAuditEventCollection> dto = orderLineAuditEventService.getAuditEventsByOrderLineId(id, "action_date", "desc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      OrderLineAuditEventCollection orderLineAuditEventOptional = ar.result();
      List<OrderLineAuditEvent> orderLineAuditEventList = orderLineAuditEventOptional.getOrderLineAuditEvents();

      assertEquals(orderLineAuditEventList.get(0).getId(), id);
      assertEquals(OrderLineAuditEvent.Action.CREATE, orderLineAuditEventList.get(0).getAction());

    });
  }

}
