package org.folio.services.impl;

import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.OrderEvenDao;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.services.OrderAuditEventService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("orderAuditEventService")
public class OrderAuditEventServiceImpl implements OrderAuditEventService {

  public static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";

  private OrderEvenDao orderEvenDao;
  @Override
  public Future<RowSet<Row>> collectData(String id, String action, String orderId, String userId,
                                         Date eventDate, Date action_date, String modifiedContent, String tenantId) {
    return orderEvenDao.save(id, action, orderId, userId, eventDate, action_date,
        modifiedContent, tenantId).recover(throwable -> handleFailures(throwable, id));
  }

  private <T> Future<T> handleFailures(Throwable throwable, String id) {
    return (throwable instanceof PgException && ((PgException) throwable).getCode().equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) ?
      Future.failedFuture(new DuplicateEventException(String.format("Event with eventId=%s is already processed.", id))) :
      Future.failedFuture(throwable);
  }
}
