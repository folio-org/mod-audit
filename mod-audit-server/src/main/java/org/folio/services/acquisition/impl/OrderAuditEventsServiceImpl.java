package org.folio.services.acquisition.impl;

import static org.folio.util.AuditEventDBConstants.UNIQUE_CONSTRAINT_VIOLATION_CODE;

import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrderEventsDao;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderAuditEventsServiceImpl implements OrderAuditEventsService {

  private static final Logger LOGGER = LogManager.getLogger();

  private OrderEventsDao orderEventsDao;

  @Autowired
  public OrderAuditEventsServiceImpl(OrderEventsDao orderEvenDao) {
    this.orderEventsDao = orderEvenDao;
  }

  @Override
  public Future<RowSet<Row>> saveOrderAuditEvent(OrderAuditEvent orderAuditEvent, String tenantId) {
    LOGGER.debug("saveOrderAuditEvent:: Saving order audit event with Id={} for tenantId={}", orderAuditEvent.getId(), tenantId);
    return orderEventsDao.save(orderAuditEvent, tenantId).recover(throwable -> handleFailures(throwable, orderAuditEvent.getId()));
  }

  @Override
  public Future<OrderAuditEventCollection> getAuditEventsByOrderId(String orderId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderId:: Retrieving audit events for orderId={} and tenantId={}", orderId, tenantId);
    return orderEventsDao.getAuditEventsByOrderId(orderId, sortBy, sortOrder, limit, offset, tenantId);
  }

  private <T> Future<T> handleFailures(Throwable throwable, String id) {
    LOGGER.debug("handleFailures:: Handling Failures with Id : {}", id);
    return (throwable instanceof PgException && ((PgException) throwable).getCode().equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) ?
      Future.failedFuture(new DuplicateEventException(String.format("Event with Id=%s is already processed.", id))) :
      Future.failedFuture(throwable);
  }

}
