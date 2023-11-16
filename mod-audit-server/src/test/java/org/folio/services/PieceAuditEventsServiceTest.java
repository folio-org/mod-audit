package org.folio.services;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createPieceAuditEvent;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.PieceEventsDao;
import org.folio.dao.acquisition.impl.PieceEventsDaoImpl;
import org.folio.services.acquisition.impl.PieceAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class PieceAuditEventsServiceTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  PieceEventsDao pieceEventsDao = new PieceEventsDaoImpl(postgresClientFactory);
  @InjectMocks
  PieceAuditEventsServiceImpl pieceAuditEventsService = new PieceAuditEventsServiceImpl(pieceEventsDao);

  @Test
  void shouldCallDaoSuccessfully() {
    var pieceAuditEvent = createPieceAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = pieceAuditEventsService.savePieceAuditEvent(pieceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }
}
