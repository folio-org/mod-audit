package org.folio.services.acquisition.impl;

import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrderEventsDao;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.springframework.stereotype.Service;

@Service
public class OrderAuditEventsServiceImpl implements OrderAuditEventsService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final OrderEventsDao orderEventsDao;

  public OrderAuditEventsServiceImpl(OrderEventsDao orderEvenDao) {
    this.orderEventsDao = orderEvenDao;
  }

  @Override
  public Future<RowSet<Row>> saveOrderAuditEvent(OrderAuditEvent orderAuditEvent, String tenantId) {
    LOGGER.debug("saveOrderAuditEvent:: Saving order audit event with orderId={} for tenantId={}", orderAuditEvent.getOrderId(), tenantId);
    return orderEventsDao.save(orderAuditEvent, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save order audit event for Order id: {} in tenantId: {}", orderAuditEvent.getOrderId(), tenantId);
        return handleFailures(throwable, orderAuditEvent.getId());
      });
  }

  @Override
  public Future<OrderAuditEventCollection> getAuditEventsByOrderId(String orderId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderId:: Retrieving audit events for orderId={} and tenantId={}", orderId, tenantId);
    return orderEventsDao.getAuditEventsByOrderId(orderId, sortBy, sortOrder, limit, offset, tenantId);
  }
}
