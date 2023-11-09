package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createPieceAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.impl.PieceEventsDaoImpl;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class PieceEventsDaoTest {

  @Spy
  PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  PieceEventsDaoImpl pieceEventsDao = new PieceEventsDaoImpl(postgresClientFactory);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    pieceEventsDao = new PieceEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test product 123");

    PieceAuditEvent pieceAuditEvent = new PieceAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withPieceId(UUID.randomUUID().toString())
      .withActionDate(new Date())
      .withAction(PieceAuditEvent.Action.CREATE)
      .withPieceSnapshot(jsonObject);

    Future<RowSet<Row>> saveFuture = pieceEventsDao.save(pieceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldThrowConstrainViolation() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test product 123");

    PieceAuditEvent pieceAuditEvent = new PieceAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withPieceId(UUID.randomUUID().toString())
      .withActionDate(new Date())
      .withAction(PieceAuditEvent.Action.CREATE)
      .withPieceSnapshot(jsonObject);

    Future<RowSet<Row>> saveFuture = pieceEventsDao.save(pieceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = pieceEventsDao.save(pieceAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_piece_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
  }

  @Test
  void shouldGetCreatedEvent() {
    String id = UUID.randomUUID().toString();
    var pieceAuditEvent = createPieceAuditEvent(id);

    pieceEventsDao.save(pieceAuditEvent, TENANT_ID);

    Future<PieceAuditEventCollection> saveFuture = pieceEventsDao.getAuditEventsByPieceId(id, "action_date", "desc", 1, 1, TENANT_ID);
    saveFuture.onComplete(ar -> {
      PieceAuditEventCollection pieceAuditEventCollection = ar.result();
      List<PieceAuditEvent> pieceAuditEventList = pieceAuditEventCollection.getPieceAuditEvents();
      assertEquals(pieceAuditEventList.get(0).getId(), id);
      assertEquals(PieceAuditEvent.Action.CREATE.value(), pieceAuditEventList.get(0).getAction().value());
    });
  }
}
