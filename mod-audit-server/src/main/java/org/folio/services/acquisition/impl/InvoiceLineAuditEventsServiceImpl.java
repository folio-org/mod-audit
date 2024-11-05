package org.folio.services.acquisition.impl;

import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.InvoiceLineEventsDao;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEventCollection;
import org.folio.services.acquisition.InvoiceLineAuditEventsService;
import org.springframework.stereotype.Service;

@Service
public class InvoiceLineAuditEventsServiceImpl implements InvoiceLineAuditEventsService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final InvoiceLineEventsDao invoiceLineEventsDao;

  public InvoiceLineAuditEventsServiceImpl(InvoiceLineEventsDao invoiceLineEventsDao) {
    this.invoiceLineEventsDao = invoiceLineEventsDao;
  }

  @Override
  public Future<RowSet<Row>> saveInvoiceLineAuditEvent(InvoiceLineAuditEvent invoiceLineAuditEvent, String tenantId) {
    LOGGER.debug("saveInvoiceLineAuditEvent:: Saving invoice line audit event with invoiceLineId={} in tenant Id={}", invoiceLineAuditEvent.getId(), tenantId);
    return invoiceLineEventsDao.save(invoiceLineAuditEvent, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save invoice audit event for InvoiceLine id: {} in tenantId: {}", invoiceLineAuditEvent.getInvoiceLineId(), tenantId);
        return handleFailures(throwable, invoiceLineAuditEvent.getId());
      });
  }

  @Override
  public Future<InvoiceLineAuditEventCollection> getAuditEventsByInvoiceLineId(String invoiceLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByInvoiceLineId:: Retrieving audit events for invoiceLineId={} and tenantId={}", invoiceLineId, tenantId);
    return invoiceLineEventsDao.getAuditEventsByInvoiceLineId(invoiceLineId, sortBy, sortOrder, limit, offset, tenantId);
  }
}
