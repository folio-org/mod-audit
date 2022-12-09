package org.folio.dao;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderLineEventsDaoTest {

  private static final String TENANT_ID = "diku";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  OrderLineEventsDaoImpl orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);
  }

  @Test
  public void shouldCreateEventProcessed() {
    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withOrderLineId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderLineSnapshot("{\"name\":\"New OrderLine Product\"}");

    Future<RowSet<Row>> saveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
        assertTrue(ar.succeeded());
      });
  }

  @Test
  public  void shouldThrowConstraintViolation() {
    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withOrderLineId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderLineSnapshot("{\"name\":\"New Product\"}");

    Future<RowSet<Row>> saveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = orderLineEventsDao.save(orderLineAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof  PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_order_line_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
    };

}


