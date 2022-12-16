package org.folio.dao;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.folio.util.PostgresClientFactory;

import java.util.Date;
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
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product");

    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);

    Future<RowSet<Row>> saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
        assertTrue(ar.succeeded());
      });
  }

  @Test
  public  void shouldThrowConstraintViolation() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product");

    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);

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
  void shouldGetCreatedEvent() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product");
    String id = UUID.randomUUID().toString();

    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(id)
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test")
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);

    orderEventDao.save(orderAuditEvent, TENANT_ID);

    Future<Optional<OrderAuditEventCollection>> getFuture = orderEventDao.getAcquisitionOrderAuditEventById(id, TENANT_ID);
    getFuture.onComplete(ar-> {
      assertTrue(ar.succeeded());
    });
  }

}


