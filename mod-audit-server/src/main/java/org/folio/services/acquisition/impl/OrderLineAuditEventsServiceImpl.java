package org.folio.services.acquisition.impl;

import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.OrderLineEventsDao;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderLineAuditEventsServiceImpl implements OrderLineAuditEventsService {

  public static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";

  private OrderLineEventsDao orderLineEventsDao;

  @Autowired
  public OrderLineAuditEventsServiceImpl(OrderLineEventsDao orderLineEventsDao) {
    this.orderLineEventsDao = orderLineEventsDao;
  }

  @Override
  public Future<RowSet<Row>> saveOrderLineAuditEvent(OrderLineAuditEvent orderLineAuditEvent, String tenantId) {
    return orderLineEventsDao.save(orderLineAuditEvent, tenantId).recover(throwable -> handleFailures(throwable, orderLineAuditEvent.getId()));
  }

  @Override
  public Future<OrderLineAuditEventCollection> getAuditEventsByOrderLineId(String orderLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    return orderLineEventsDao.getAuditEventsByOrderLineId(orderLineId, sortBy, sortOrder, limit, offset, tenantId);
  }

  private <T> Future<T> handleFailures(Throwable throwable, String id) {
    return (throwable instanceof PgException && ((PgException) throwable).getCode().equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) ?
      Future.failedFuture(new DuplicateEventException(String.format("Event with Id=%s is already processed.", id))) :
      Future.failedFuture(throwable);
  }

}
