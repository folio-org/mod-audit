package org.folio.services;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInvoiceAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.InvoiceEventsDao;
import org.folio.dao.acquisition.impl.InvoiceEventsDaoImpl;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceAuditEventCollection;
import org.folio.services.acquisition.impl.InvoiceAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@CopilotGenerated
public class InvoiceAuditEventsServiceTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @Mock
  InvoiceEventsDao invoiceEventsDao = new InvoiceEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  InvoiceAuditEventsServiceImpl invoiceAuditEventService = new InvoiceAuditEventsServiceImpl(invoiceEventsDao);

  @Test
  void shouldCallDaoForSuccessfulCase() {
    var invoiceAuditEvent = createInvoiceAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = invoiceAuditEventService.saveInvoiceAuditEvent(invoiceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldGetDto() {
    String id = UUID.randomUUID().toString();
    var invoiceAuditEvent = createInvoiceAuditEvent(id);

    invoiceAuditEventService.saveInvoiceAuditEvent(invoiceAuditEvent, TENANT_ID);

    Future<InvoiceAuditEventCollection> dto = invoiceAuditEventService.getAuditEventsByInvoiceId(id, "action_date", "asc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      InvoiceAuditEventCollection invoiceAuditEventOptional = ar.result();
      List<InvoiceAuditEvent> invoiceAuditEventList = invoiceAuditEventOptional.getInvoiceAuditEvents();

      assertEquals(invoiceAuditEventList.get(0).getId(), id);
      assertEquals(InvoiceAuditEvent.Action.CREATE.value(), invoiceAuditEventList.get(0).getAction().value());
    });
  }
}
