package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.OrderEventsDao;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.services.acquisition.impl.OrderAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.LocalDateTime;
import java.util.UUID;

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
  public void shouldCallDaoForSuccessfulCase() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","jsonTest");
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderSnapshot("{\"name\":\"New Product\"}");

    Future<RowSet<Row>> saveFuture = orderAuditEventService.saveOrderAuditEvent(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

}
