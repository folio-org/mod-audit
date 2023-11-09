package org.folio.services;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
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

public class OrderAuditEventsServiceTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  OrderEventsDao orderEventsDao = new OrderEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  OrderAuditEventsServiceImpl orderAuditEventService = new OrderAuditEventsServiceImpl(orderEventsDao);

  @Test
  void shouldCallDaoForSuccessfulCase() {
    var orderAuditEvent = createOrderAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = orderAuditEventService.saveOrderAuditEvent(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldGetDto() {
    String id = UUID.randomUUID().toString();
    var orderAuditEvent = createOrderAuditEvent(id);

    orderAuditEventService.saveOrderAuditEvent(orderAuditEvent, TENANT_ID);

    Future<OrderAuditEventCollection> dto = orderAuditEventService.getAuditEventsByOrderId(id, "action_date", "asc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      OrderAuditEventCollection orderAuditEventOptional = ar.result();
      List<OrderAuditEvent> orderAuditEventList = orderAuditEventOptional.getOrderAuditEvents();

      assertEquals(orderAuditEventList.get(0).getId(), id);
      assertEquals(OrderAuditEvent.Action.CREATE.value(), orderAuditEventList.get(0).getAction().value());
    });
  }

}
