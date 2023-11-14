package org.folio.services.acquisition.impl;

import static org.folio.util.AuditEventDBConstants.UNIQUE_CONSTRAINT_VIOLATION_CODE;

import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrderLineEventsDao;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderLineAuditEventsServiceImpl implements OrderLineAuditEventsService {

  private static final Logger LOGGER = LogManager.getLogger();

  private OrderLineEventsDao orderLineEventsDao;

  @Autowired
  public OrderLineAuditEventsServiceImpl(OrderLineEventsDao orderLineEventsDao) {
    this.orderLineEventsDao = orderLineEventsDao;
  }

  @Override
  public Future<RowSet<Row>> saveOrderLineAuditEvent(OrderLineAuditEvent orderLineAuditEvent, String tenantId) {
    LOGGER.debug("saveOrderLineAuditEvent:: Saving order line audit event with tenant Id : {}", tenantId);
    return orderLineEventsDao.save(orderLineAuditEvent, tenantId).recover(throwable -> handleFailures(throwable, orderLineAuditEvent.getId()));
  }

  @Override
  public Future<OrderLineAuditEventCollection> getAuditEventsByOrderLineId(String orderLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderLineId:: Retrieving audit events for order line Id : {} and tenant Id : {}", orderLineId, tenantId);
    return orderLineEventsDao.getAuditEventsByOrderLineId(orderLineId, sortBy, sortOrder, limit, offset, tenantId);
  }

  private <T> Future<T> handleFailures(Throwable throwable, String id) {
    LOGGER.debug("handleFailures:: Handling Failures with Id : {}", id);
    return (throwable instanceof PgException && ((PgException) throwable).getCode().equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) ?
      Future.failedFuture(new DuplicateEventException(String.format("Event with Id=%s is already processed.", id))) :
      Future.failedFuture(throwable);
  }

}
