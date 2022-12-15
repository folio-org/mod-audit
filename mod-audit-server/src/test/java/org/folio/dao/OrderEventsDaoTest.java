package org.folio.dao;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventDto;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.folio.util.PostgresClientFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderEventsDaoTest {

  private static final String TENANT_ID = "diku";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  OrderEventsDaoImpl orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);
  }

  @Test
  public void shouldCreateEventProcessed() {
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderSnapshot("{\"name\":\"New Product\"}");

    Future<RowSet<Row>> saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
        assertTrue(ar.succeeded());
      });
  }

  @Test
  public  void shouldThrowConstraintViolation() {
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderSnapshot("{\"name\":\"New Product\"}");

    Future<RowSet<Row>> saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof  PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_order_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
    }

  @Test
  public void shouldGetCreatedEvent() {
    String id = UUID.randomUUID().toString();
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(id)
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderSnapshot("{\"name\":\"New Product\"}");

    orderEventDao.save(orderAuditEvent, TENANT_ID);

    Future<Optional<OrderAuditEventDto>> getFuture = orderEventDao.getAcquisitionOrderAuditEventById(id, TENANT_ID);
    getFuture.onComplete(ar->{
      assertTrue(ar.succeeded());
    });
  }

}


