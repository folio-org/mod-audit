package org.folio.services.impl;

import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.OrderEventDao;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.services.OrderAuditEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("orderAuditEventService")
public class OrderAuditEventServiceImpl implements OrderAuditEventService {

  public static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";

  private OrderEventDao orderEvenDao;

  @Autowired
  public OrderAuditEventServiceImpl(OrderEventDao orderEvenDao) {
    this.orderEvenDao = orderEvenDao;
  }

  @Override
  public Future<RowSet<Row>> collectData(OrderAuditEvent orderAuditEvent, String tenantId) {
    return orderEvenDao.save(orderAuditEvent, tenantId).recover(throwable -> handleFailures(throwable, orderAuditEvent.getId()));
  }

  private <T> Future<T> handleFailures(Throwable throwable, String id) {
    return (throwable instanceof PgException && ((PgException) throwable).getCode().equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) ?
      Future.failedFuture(new DuplicateEventException(String.format("Event with eventId=%s is already processed.", id))) :
      Future.failedFuture(throwable);
  }

}
