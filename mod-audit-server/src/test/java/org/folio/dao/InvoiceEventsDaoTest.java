package org.folio.dao;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createInvoiceAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.impl.InvoiceEventsDaoImpl;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@CopilotGenerated
public class InvoiceEventsDaoTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  InvoiceEventsDaoImpl invoiceEventDao = new InvoiceEventsDaoImpl(postgresClientFactory);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    invoiceEventDao = new InvoiceEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var invoiceAuditEvent = createInvoiceAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = invoiceEventDao.save(invoiceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
      verify(postgresClientFactory, times(1)).createInstance(TENANT_ID).execute(anyString(), any(Tuple.class));
    });
  }

  @Test
  void shouldThrowConstraintViolation() {
    var invoiceAuditEvent = createInvoiceAuditEvent(UUID.randomUUID().toString());

    Future<RowSet<Row>> saveFuture = invoiceEventDao.save(invoiceAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      Future<RowSet<Row>> reSaveFuture = invoiceEventDao.save(invoiceAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_invoice_log_pkey\" (23505)", re.cause().getMessage());
        verify(postgresClientFactory, times(1)).createInstance(TENANT_ID).execute(anyString(), any(Tuple.class));
      });
    });
  }

  @Test
  void shouldGetCreatedEvent() {
    String id = UUID.randomUUID().toString();
    var invoiceAuditEvent = createInvoiceAuditEvent(id);

    invoiceEventDao.save(invoiceAuditEvent, TENANT_ID);

    Future<InvoiceAuditEventCollection> dto = invoiceEventDao.getAuditEventsByInvoiceId(id, "action_date", "desc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      InvoiceAuditEventCollection invoiceAuditEventOptional = ar.result();
      List<InvoiceAuditEvent> invoiceAuditEventList = invoiceAuditEventOptional.getInvoiceAuditEvents();

      assertEquals(invoiceAuditEventList.get(0).getId(), id);
      assertEquals(InvoiceAuditEvent.Action.CREATE.value(), invoiceAuditEventList.get(0).getAction().value());
      verify(postgresClientFactory, times(1)).createInstance(TENANT_ID).selectRead(anyString(), any(Tuple.class), any());
    });
  }
}
