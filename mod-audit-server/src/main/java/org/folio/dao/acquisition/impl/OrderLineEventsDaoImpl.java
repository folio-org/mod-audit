package org.folio.dao.acquisition.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrderLineEventsDao;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static java.lang.String.format;

@Repository
public class OrderLineEventsDaoImpl implements OrderLineEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_order_line_log";

  private static final String INSERT_SQL = "INSERT INTO %s (id, action, order_id, order_line_id, user_id, user_name, event_date, action_date, modified_content_snapshot) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)";

  @Autowired
  private final PostgresClientFactory pgClientFactory;

  @Autowired
  public OrderLineEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(OrderLineAuditEvent orderLineAuditEvent, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();

    String query = format(INSERT_SQL, TABLE_NAME);

    makeSaveCall(promise, query, orderLineAuditEvent, tenantId);

    return promise.future();
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, OrderLineAuditEvent orderLineAuditEvent, String tenantId) {
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(orderLineAuditEvent.getId(),
        orderLineAuditEvent.getAction(),
        orderLineAuditEvent.getOrderId(),
        orderLineAuditEvent.getOrderLineId(),
        orderLineAuditEvent.getUserId(),
        orderLineAuditEvent.getUserName(),
        orderLineAuditEvent.getEventDate(),
        orderLineAuditEvent.getActionDate(),
        new JsonObject(orderLineAuditEvent.getOrderLineSnapshot())), promise);
    } catch (Exception e) {
      LOGGER.error("Failed to save record with Id {} in to table {}", orderLineAuditEvent.getId(), TABLE_NAME, e);
      promise.fail(e);
    }
  }
}
