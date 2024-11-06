package org.folio.services;

import static org.folio.utils.EntityUtils.ACTION_DATE_SORT_BY;
import static org.folio.utils.EntityUtils.DESC_ORDER;
import static org.folio.utils.EntityUtils.LIMIT;
import static org.folio.utils.EntityUtils.OFFSET;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInvoiceLineAuditEvent;
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
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.InvoiceLineEventsDao;
import org.folio.dao.acquisition.impl.InvoiceLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEventCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.acquisition.impl.InvoiceLineAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@CopilotGenerated
public class InvoiceLineAuditEventsServiceTest {

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private PostgresClient postgresClient;

  private InvoiceLineEventsDao invoiceLineEventsDao;
  private InvoiceLineAuditEventsServiceImpl invoiceLineAuditEventService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory = spy(new PostgresClientFactory(Vertx.vertx()));
      invoiceLineEventsDao = spy(new InvoiceLineEventsDaoImpl(postgresClientFactory));
      invoiceLineAuditEventService = new InvoiceLineAuditEventsServiceImpl(invoiceLineEventsDao);

      doReturn(postgresClient).when(postgresClientFactory).createInstance(TENANT_ID);
    }
  }

  @Test
  public void shouldCallDaoForSuccessfulCase() {
    var invoiceLineAuditEvent = createInvoiceLineAuditEvent(UUID.randomUUID().toString());
    doReturn(Future.succeededFuture(rowSet)).when(postgresClient).execute(anyString(), any(Tuple.class));

    var saveFuture = invoiceLineAuditEventService.saveInvoiceLineAuditEvent(invoiceLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(invoiceLineEventsDao, times(1)).save(invoiceLineAuditEvent, TENANT_ID);
  }

  @Test
  void shouldGetInvoiceLineDto() {
    var id = UUID.randomUUID().toString();
    var invoiceLineAuditEvent = createInvoiceLineAuditEvent(id);
    var invoiceLineAuditEventCollection = new InvoiceLineAuditEventCollection().withInvoiceLineAuditEvents(List.of(invoiceLineAuditEvent)).withTotalItems(1);

    doReturn(Future.succeededFuture(invoiceLineAuditEventCollection)).when(invoiceLineEventsDao).getAuditEventsByInvoiceLineId(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

    var dto = invoiceLineAuditEventService.getAuditEventsByInvoiceLineId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
    dto.onComplete(asyncResult -> {
      var invoiceLineAuditEventOptional = asyncResult.result();
      var invoiceLineAuditEventList = invoiceLineAuditEventOptional.getInvoiceLineAuditEvents();

      assertEquals(invoiceLineAuditEventList.get(0).getId(), id);
      assertEquals(InvoiceLineAuditEvent.Action.CREATE, invoiceLineAuditEventList.get(0).getAction());
    });

    verify(invoiceLineEventsDao, times(1)).getAuditEventsByInvoiceLineId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
  }
}
