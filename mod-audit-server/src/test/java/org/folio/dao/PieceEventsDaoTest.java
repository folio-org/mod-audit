package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createPieceAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import org.folio.dao.acquisition.impl.PieceEventsDaoImpl;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
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
  PieceEventsDaoImpl pieceEventsDao;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    pieceEventsDao = new PieceEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var pieceAuditEvent = createPieceAuditEvent(UUID.randomUUID().toString());

    var saveFuture = pieceEventsDao.save(pieceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldThrowConstrainViolation() {
    var pieceAuditEvent = createPieceAuditEvent(UUID.randomUUID().toString());

    var saveFuture = pieceEventsDao.save(pieceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      var reSaveFuture = pieceEventsDao.save(pieceAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_piece_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldGetCreatedEvent() {
    var id = UUID.randomUUID().toString();
    var pieceAuditEvent = createPieceAuditEvent(id);

    pieceEventsDao.save(pieceAuditEvent, TENANT_ID);

    var saveFuture = pieceEventsDao.getAuditEventsByPieceId(id, "action_date", "desc", 1, 1, TENANT_ID);
    saveFuture.onComplete(ar -> {
      var pieceAuditEventCollection = ar.result();
      var pieceAuditEventList = pieceAuditEventCollection.getPieceAuditEvents();

      assertEquals(pieceAuditEventList.get(0).getId(), id);
      assertEquals(PieceAuditEvent.Action.CREATE.value(), pieceAuditEventList.get(0).getAction().value());
    });
    verify(postgresClientFactory, times(2)).createInstance(TENANT_ID);
  }
}
