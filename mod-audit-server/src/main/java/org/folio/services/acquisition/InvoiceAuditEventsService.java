package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceAuditEventCollection;

public interface InvoiceAuditEventsService {

  /**
   * Saves InvoiceAuditEvent
   *
   * @param invoiceAuditEvent
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> saveInvoiceAuditEvent(InvoiceAuditEvent invoiceAuditEvent, String tenantId);

  /**
   * Searches for invoice audit events by invoice id
   *
   * @param invoiceId   invoice id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @return future with InvoiceAuditEventCollection
   */
  Future<InvoiceAuditEventCollection> getAuditEventsByInvoiceId(String invoiceId, String sortBy, String sortOrder, int limit, int offset, String tenantId);
}
