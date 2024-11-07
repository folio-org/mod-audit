package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEventCollection;

public interface InvoiceLineEventsDao {

  /**
   * Saves invoiceLineAuditEvent entity to DB
   *
   * @param invoiceLineAuditEvent InvoiceLineAuditEvent entity to save
   * @param tenantId tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(InvoiceLineAuditEvent invoiceLineAuditEvent, String tenantId);

  /**
   * Searches for invoice_line audit events by id
   *
   * @param invoiceLineId invoice_line id
   * @param sortBy        sort by
   * @param sortOrder     sort order
   * @param limit         limit
   * @param offset        offset
   * @param tenantId      tenant id
   * @return future with InvoiceLineAuditEventCollection
   */
  Future<InvoiceLineAuditEventCollection> getAuditEventsByInvoiceLineId(String invoiceLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId);
}
