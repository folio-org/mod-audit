package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.OrderEventDao;
import org.folio.dao.impl.OrderEventDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.services.impl.OrderAuditEventServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderAuditEventServiceTest {

  private static final String TENANT_ID = "diku";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  OrderEventDao orderEventDao = new OrderEventDaoImpl(postgresClientFactory);

  @InjectMocks
  OrderAuditEventServiceImpl orderAuditEventService = new OrderAuditEventServiceImpl(orderEventDao);

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

    Future<RowSet<Row>> saveFuture = orderAuditEventService.collectData(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });

//    when(orderEventDao.save(orderAuditEvent, TENANT_ID)).thenReturn(Future.succeededFuture());
//
//    orderAuditEventService.collectData(orderAuditEvent, TENANT_ID);
//
//    verify(orderEventDao).save(orderAuditEvent, TENANT_ID);
  }

}
