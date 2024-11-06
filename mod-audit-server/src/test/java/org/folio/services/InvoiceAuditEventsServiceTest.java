package org.folio.services;

import static org.folio.utils.EntityUtils.ACTION_DATE_SORT_BY;
import static org.folio.utils.EntityUtils.DESC_ORDER;
import static org.folio.utils.EntityUtils.LIMIT;
import static org.folio.utils.EntityUtils.OFFSET;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInvoiceAuditEvent;
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
import org.folio.dao.acquisition.InvoiceEventsDao;
import org.folio.dao.acquisition.impl.InvoiceEventsDaoImpl;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceAuditEventCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.acquisition.impl.InvoiceAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@CopilotGenerated
public class InvoiceAuditEventsServiceTest {

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private PostgresClient postgresClient;

  private InvoiceEventsDao invoiceEventsDao;
  private InvoiceAuditEventsServiceImpl invoiceAuditEventService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory =  spy(new PostgresClientFactory(Vertx.vertx()));
      invoiceEventsDao = spy(new InvoiceEventsDaoImpl(postgresClientFactory));
      invoiceAuditEventService = new InvoiceAuditEventsServiceImpl(invoiceEventsDao);

      doReturn(postgresClient).when(postgresClientFactory).createInstance(TENANT_ID);
    }
  }

  @Test
  void shouldCallDaoForSuccessfulCase() {
    var invoiceAuditEvent = createInvoiceAuditEvent(UUID.randomUUID().toString());
    doReturn(Future.succeededFuture(rowSet)).when(postgresClient).execute(anyString(), any(Tuple.class));

    var saveFuture = invoiceAuditEventService.saveInvoiceAuditEvent(invoiceAuditEvent, TENANT_ID);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(invoiceEventsDao, times(1)).save(invoiceAuditEvent, TENANT_ID);
  }

  @Test
  void shouldGetDto() {
    var id = UUID.randomUUID().toString();
    var invoiceAuditEvent = createInvoiceAuditEvent(id);
    var invoiceAuditEventCollection = new InvoiceAuditEventCollection().withInvoiceAuditEvents(List.of(invoiceAuditEvent)).withTotalItems(1);

    doReturn(Future.succeededFuture(invoiceAuditEventCollection)).when(invoiceEventsDao).getAuditEventsByInvoiceId(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

    var dto = invoiceAuditEventService.getAuditEventsByInvoiceId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
    dto.onComplete(asyncResult -> {
      var invoiceAuditEventOptional = asyncResult.result();
      var invoiceAuditEventList = invoiceAuditEventOptional.getInvoiceAuditEvents();

      assertEquals(invoiceAuditEventList.get(0).getId(), id);
      assertEquals(InvoiceAuditEvent.Action.CREATE, invoiceAuditEventList.get(0).getAction());
    });

    verify(invoiceEventsDao, times(1)).getAuditEventsByInvoiceId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
  }
}
