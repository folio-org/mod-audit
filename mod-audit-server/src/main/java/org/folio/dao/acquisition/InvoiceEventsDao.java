package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceAuditEventCollection;

public interface InvoiceEventsDao {

  /**
   * Saves invoiceAuditEvent entity to DB
   *
   * @param invoiceAuditEvent InvoiceAuditEvent entity to save
   * @param tenantId tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(InvoiceAuditEvent invoiceAuditEvent, String tenantId);

  /**
   * Searches for invoice audit events by id
   *
   * @param invoiceId   invoice id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @param tenantId  tenant id
   * @return future with InvoiceAuditEventCollection
   */
  Future<InvoiceAuditEventCollection> getAuditEventsByInvoiceId(String invoiceId, String sortBy, String sortOrder, int limit, int offset, String tenantId);
}
