package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderLineAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
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
  OrderLineEventsDaoImpl orderLineEventsDao;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var orderLineAuditEvent = createOrderLineAuditEvent(UUID.randomUUID().toString());

    var saveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldThrowConstraintViolation() {
    var orderLineAuditEvent = createOrderLineAuditEvent(UUID.randomUUID().toString());

    var saveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      var reSaveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof  PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_order_line_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldGetCreatedEvent() {
    var id = UUID.randomUUID().toString();
    var orderLineAuditEvent = createOrderLineAuditEvent(id);

    orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);

    var dto = orderLineEventsDao.getAuditEventsByOrderLineId(id, "action_date",  "asc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      var orderLineAuditEventOptional = ar.result();
      var orderLineAuditEventList = orderLineAuditEventOptional.getOrderLineAuditEvents();

      assertEquals(orderLineAuditEventList.get(0).getId(), id);
      assertEquals(OrderAuditEvent.Action.CREATE.value(), orderLineAuditEventList.get(0).getAction().value());
    });
    verify(postgresClientFactory, times(2)).createInstance(TENANT_ID);
  }
}


