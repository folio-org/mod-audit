package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEventCollection;

public interface InvoiceLineAuditEventsService {

  /**
   * Saves InvoiceLineAuditEvent
   *
   * @param invoiceLineAuditEvent invoice line event to save
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> saveInvoiceLineAuditEvent(InvoiceLineAuditEvent invoiceLineAuditEvent, String tenantId);

  /**
   * Searches for invoice line audit events by invoice line id
   *
   * @param invoiceLineId invoice line id
   * @param sortBy        sort by
   * @param sortOrder     sort order
   * @param limit         limit
   * @param offset        offset
   * @return future with InvoiceLineAuditEventCollection
   */
  Future<InvoiceLineAuditEventCollection> getAuditEventsByInvoiceLineId(String invoiceLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId);
}
