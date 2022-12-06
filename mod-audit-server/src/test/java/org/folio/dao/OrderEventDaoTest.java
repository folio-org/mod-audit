package org.folio.dao;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.impl.OrderEventDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.services.impl.OrderAuditEventServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.folio.util.PostgresClientFactory;

import java.time.LocalDateTime;
import java.util.UUID;

@RunWith(VertxUnitRunner.class)
public class OrderEventDaoTest {

  private static final String TENANT_ID = "diku";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  OrderEventDaoImpl orderEventDao;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderEventDao = new OrderEventDaoImpl(postgresClientFactory);
  }

  @Test
  public void shouldCreateEventProcessed(TestContext context) {
    Async async = context.async();
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderSnapshot("{\"name\":\"New Product\"}");

    Future<RowSet<Row>> saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
        context.assertTrue(ar.succeeded());
        async.complete();
      });
  }

  @Test
  public void shouldThrowConstraintViolation(TestContext context) {
    Async async = context.async();
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderSnapshot("{\"name\":\"New Product\"}");

    Future<RowSet<Row>> saveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = orderEventDao.save(orderAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        context.assertTrue(re.failed());
        context.assertTrue(re.cause() instanceof  PgException);
        context.assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_order_log_pkey\" (23505)", re.cause().getMessage());
        async.complete();
      });
    });
    };

}


