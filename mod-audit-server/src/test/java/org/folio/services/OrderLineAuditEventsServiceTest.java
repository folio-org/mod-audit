package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.OrderLineEventsDao;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.services.acquisition.impl.OrderLineAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.LocalDateTime;
import java.util.UUID;

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
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderLineSnapshot("{\"name\":\"New OrderLine Product\"}");

    Future<RowSet<Row>> saveFuture = orderLineAuditEventService.saveOrderLineAuditEvent(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

}
