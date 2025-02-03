package org.folio.services.acquisition.impl;

import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrderLineEventsDao;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.springframework.stereotype.Service;

@Service
public class OrderLineAuditEventsServiceImpl implements OrderLineAuditEventsService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final OrderLineEventsDao orderLineEventsDao;

  public OrderLineAuditEventsServiceImpl(OrderLineEventsDao orderLineEventsDao) {
    this.orderLineEventsDao = orderLineEventsDao;
  }

  @Override
  public Future<RowSet<Row>> saveOrderLineAuditEvent(OrderLineAuditEvent orderLineAuditEvent, String tenantId) {
    LOGGER.debug("saveOrderLineAuditEvent:: Saving order line audit event with id: {} in tenant Id : {}", orderLineAuditEvent.getId(), tenantId);
    return orderLineEventsDao.save(orderLineAuditEvent, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save order audit event for OrderLine id: {} in tenantId: {}", orderLineAuditEvent.getOrderLineId(), tenantId);
        return handleFailures(throwable, orderLineAuditEvent.getId());
      });
  }

  @Override
  public Future<OrderLineAuditEventCollection> getAuditEventsByOrderLineId(String orderLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderLineId:: Retrieving audit events for order line Id : {} and tenant Id : {}", orderLineId, tenantId);
    return orderLineEventsDao.getAuditEventsByOrderLineId(orderLineId, sortBy, sortOrder, limit, offset, tenantId);
  }
}
