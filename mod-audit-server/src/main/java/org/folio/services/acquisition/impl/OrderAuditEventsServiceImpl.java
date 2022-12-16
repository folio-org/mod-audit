package org.folio.services.acquisition.impl;

import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.acquisition.OrderEventsDao;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderAuditEventsServiceImpl implements OrderAuditEventsService {

  public static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";

  private OrderEventsDao orderEventsDao;

  @Autowired
  public OrderAuditEventsServiceImpl(OrderEventsDao orderEvenDao) {
    this.orderEventsDao = orderEvenDao;
  }

  @Override
  public Future<RowSet<Row>> saveOrderAuditEvent(OrderAuditEvent orderAuditEvent, String tenantId) {
    return orderEventsDao.save(orderAuditEvent, tenantId).recover(throwable -> handleFailures(throwable, orderAuditEvent.getId()));
  }

  @Override
  public Future<Optional<OrderAuditEventCollection>> getAcquisitionOrderEventById(String id, String tenantId) {
    return orderEventsDao.getAcquisitionOrderAuditEventById(id, tenantId);
  }

  private <T> Future<T> handleFailures(Throwable throwable, String id) {
    return (throwable instanceof PgException && ((PgException) throwable).getCode().equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) ?
      Future.failedFuture(new DuplicateEventException(String.format("Event with Id=%s is already processed.", id))) :
      Future.failedFuture(throwable);
  }

}
