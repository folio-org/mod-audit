package org.folio.services.acquisition.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.InvoiceEventsDao;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceAuditEventCollection;
import org.folio.services.acquisition.InvoiceAuditEventsService;
import org.springframework.stereotype.Service;

import static org.folio.util.ErrorUtils.handleFailures;

@Service
public class InvoiceAuditEventsServiceImpl implements InvoiceAuditEventsService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final InvoiceEventsDao invoiceEventsDao;

  public InvoiceAuditEventsServiceImpl(InvoiceEventsDao invoiceEvenDao) {
    this.invoiceEventsDao = invoiceEvenDao;
  }

  @Override
  public Future<RowSet<Row>> saveInvoiceAuditEvent(InvoiceAuditEvent invoiceAuditEvent, String tenantId) {
    LOGGER.debug("saveInvoiceAuditEvent:: Saving invoice audit event with invoiceId={} for tenantId={}", invoiceAuditEvent.getInvoiceId(), tenantId);
    return invoiceEventsDao.save(invoiceAuditEvent, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save invoice audit event for Invoice id: {} in tenantId: {}", invoiceAuditEvent.getInvoiceId(), tenantId);
        return handleFailures(throwable, invoiceAuditEvent.getId());
      });
  }

  @Override
  public Future<InvoiceAuditEventCollection> getAuditEventsByInvoiceId(String invoiceId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByInvoiceId:: Retrieving audit events for invoiceId={} and tenantId={}", invoiceId, tenantId);
    return invoiceEventsDao.getAuditEventsByInvoiceId(invoiceId, sortBy, sortOrder, limit, offset, tenantId);
  }
}
