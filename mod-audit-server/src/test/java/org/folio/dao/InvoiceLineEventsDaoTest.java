package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInvoiceLineAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.impl.InvoiceLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@CopilotGenerated
public class InvoiceLineEventsDaoTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  InvoiceLineEventsDaoImpl invoiceLineEventsDao = new InvoiceLineEventsDaoImpl(postgresClientFactory);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    invoiceLineEventsDao = new InvoiceLineEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var invoiceLineAuditEvent = createInvoiceLineAuditEvent(UUID.randomUUID().toString());
    Future<RowSet<Row>> saveFuture = invoiceLineEventsDao.save(invoiceLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));
  }

  @Test
  void shouldThrowConstraintViolation() {
    var invoiceLineAuditEvent = createInvoiceLineAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = invoiceLineEventsDao.save(invoiceLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = invoiceLineEventsDao.save(invoiceLineAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_invoice_line_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
  }

  @Test
  void shouldGetCreatedEvent() {
    String id = UUID.randomUUID().toString();
    var invoiceLineAuditEvent = createInvoiceLineAuditEvent(id);

    invoiceLineEventsDao.save(invoiceLineAuditEvent, TENANT_ID);

    Future<InvoiceLineAuditEventCollection> dto = invoiceLineEventsDao.getAuditEventsByInvoiceLineId(id, "action_date", "asc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      InvoiceLineAuditEventCollection invoiceLineAuditEventOptional = ar.result();
      List<InvoiceLineAuditEvent> invoiceLineAuditEventList = invoiceLineAuditEventOptional.getInvoiceLineAuditEvents();

      assertEquals(invoiceLineAuditEventList.get(0).getId(), id);
      assertEquals(InvoiceLineAuditEvent.Action.CREATE.value(), invoiceLineAuditEventList.get(0).getAction().value());
    });
  }
}
