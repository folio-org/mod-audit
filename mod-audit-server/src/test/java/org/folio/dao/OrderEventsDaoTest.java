package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class OrderEventsDaoTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  OrderEventsDaoImpl orderEventDao;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var orderAuditEvent = createOrderAuditEvent(UUID.randomUUID().toString());

    var saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldThrowConstraintViolation() {
    var orderAuditEvent = createOrderAuditEvent(UUID.randomUUID().toString());

    var saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      var reSaveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_order_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldGetCreatedEvent() {
    String id = UUID.randomUUID().toString();
    var orderAuditEvent = createOrderAuditEvent(id);

    orderEventDao.save(orderAuditEvent, TENANT_ID);

    var dto = orderEventDao.getAuditEventsByOrderId(id, "action_date", "desc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      var orderAuditEventOptional = ar.result();
      var orderAuditEventList = orderAuditEventOptional.getOrderAuditEvents();

      assertEquals(orderAuditEventList.get(0).getId(), id);
      assertEquals(OrderAuditEvent.Action.CREATE.value(), orderAuditEventList.get(0).getAction().value());
    });
    verify(postgresClientFactory, times(2)).createInstance(TENANT_ID);
  }
}


