package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.InvoiceLineEventsDao;
import org.folio.dao.acquisition.impl.InvoiceLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEventCollection;
import org.folio.services.acquisition.impl.InvoiceLineAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.List;
import java.util.UUID;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInvoiceLineAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CopilotGenerated
public class InvoiceLineAuditEventsServiceTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  InvoiceLineEventsDao invoiceLineEventsDao = new InvoiceLineEventsDaoImpl(postgresClientFactory);
  @InjectMocks
  InvoiceLineAuditEventsServiceImpl invoiceLineAuditEventService = new InvoiceLineAuditEventsServiceImpl(invoiceLineEventsDao);

  @Test
  public void shouldCallDaoForSuccessfulCase() {
    var invoiceLineAuditEvent = createInvoiceLineAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = invoiceLineAuditEventService.saveInvoiceLineAuditEvent(invoiceLineAuditEvent, TENANT_ID);

    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldGetInvoiceLineDto() {
    String id = UUID.randomUUID().toString();
    var invoiceLineAuditEvent = createInvoiceLineAuditEvent(id);

    invoiceLineAuditEventService.saveInvoiceLineAuditEvent(invoiceLineAuditEvent, TENANT_ID);

    Future<InvoiceLineAuditEventCollection> dto = invoiceLineAuditEventService.getAuditEventsByInvoiceLineId(id, "action_date", "desc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      InvoiceLineAuditEventCollection invoiceLineAuditEventOptional = ar.result();
      List<InvoiceLineAuditEvent> invoiceLineAuditEventList = invoiceLineAuditEventOptional.getInvoiceLineAuditEvents();

      assertEquals(invoiceLineAuditEventList.get(0).getId(), id);
      assertEquals(InvoiceLineAuditEvent.Action.CREATE, invoiceLineAuditEventList.get(0).getAction());
    });
  }
}
