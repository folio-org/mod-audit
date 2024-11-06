package org.folio.services;

import static org.folio.utils.EntityUtils.ACTION_DATE_SORT_BY;
import static org.folio.utils.EntityUtils.DESC_ORDER;
import static org.folio.utils.EntityUtils.LIMIT;
import static org.folio.utils.EntityUtils.OFFSET;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createPieceAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.dao.acquisition.PieceEventsDao;
import org.folio.dao.acquisition.impl.PieceEventsDaoImpl;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEventCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.acquisition.impl.PieceAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PieceAuditEventsServiceTest {

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private PostgresClient postgresClient;

  private PieceEventsDao pieceEventsDao;
  private PieceAuditEventsServiceImpl pieceAuditEventsService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory =  spy(new PostgresClientFactory(Vertx.vertx()));
      pieceEventsDao = spy(new PieceEventsDaoImpl(postgresClientFactory));
      pieceAuditEventsService = new PieceAuditEventsServiceImpl(pieceEventsDao);

      doReturn(postgresClient).when(postgresClientFactory).createInstance(TENANT_ID);
    }
  }

  @Test
  void shouldCallDaoSuccessfully() {
    var pieceAuditEvent = createPieceAuditEvent(UUID.randomUUID().toString());
    doReturn(Future.succeededFuture(rowSet)).when(postgresClient).execute(anyString(), any(Tuple.class));

    var saveFuture = pieceAuditEventsService.savePieceAuditEvent(pieceAuditEvent, TENANT_ID);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(pieceEventsDao, times(1)).save(pieceAuditEvent, TENANT_ID);
  }

  @Test
  void shouldGetDto() {
    var id = UUID.randomUUID().toString();
    var pieceAuditEvent = createPieceAuditEvent(id);
    var pieceAuditEventCollection = new PieceAuditEventCollection().withPieceAuditEvents(List.of(pieceAuditEvent)).withTotalItems(1);

    doReturn(Future.succeededFuture(pieceAuditEventCollection)).when(pieceEventsDao).getAuditEventsByPieceId(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

    var dto = pieceAuditEventsService.getAuditEventsByPieceId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
    dto.onComplete(asyncResult -> {
      var pieceAuditEventOptional = asyncResult.result();
      var pieceAuditEventList = pieceAuditEventOptional.getPieceAuditEvents();

      assertEquals(pieceAuditEventList.get(0).getId(), id);
      assertEquals(PieceAuditEvent.Action.CREATE, pieceAuditEventList.get(0).getAction());
    });

    verify(pieceEventsDao, times(1)).getAuditEventsByPieceId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
  }
}
