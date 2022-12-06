package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.OrderEventDao;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static java.lang.String.format;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

@Repository
public class OrderEventDaoImpl implements OrderEventDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_order_log";

  private static final String INSERT_SQL = "INSERT INTO %s (id, action, order_id, user_id, event_date, action_date, modified_content_snapshot) VALUES ($1, $2, $3, $4, $5, $6, $7)";

  @Autowired
  private final PostgresClientFactory pgClientFactory;

  @Autowired
  public OrderEventDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(OrderAuditEvent orderAuditEvent, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();

    String query = format(INSERT_SQL, TABLE_NAME);

    makeSaveCall(promise, query, orderAuditEvent, tenantId);

    return promise.future();
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, OrderAuditEvent orderAuditEvent, String tenantId) {
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(orderAuditEvent.getId(),
        orderAuditEvent.getAction(),
        orderAuditEvent.getOrderId(),
        orderAuditEvent.getUserId(),
        orderAuditEvent.getEventDate(),
        orderAuditEvent.getActionDate(),
        new JsonObject(orderAuditEvent.getOrderSnapshot())), promise);
    } catch (Exception e) {
      LOGGER.error("Failed to save record with Id {} in to table {}", orderAuditEvent.getId(), TABLE_NAME, e);
      promise.fail(e);
    }
  }
}
