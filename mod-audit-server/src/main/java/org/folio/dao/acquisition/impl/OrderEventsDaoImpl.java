package org.folio.dao.acquisition.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrderEventsDao;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static java.lang.String.format;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;
import static org.folio.util.OrderAuditEventDBConstants.*;

@Repository
public class OrderEventsDaoImpl implements OrderEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_order_log";

  public static final String GET_BY_ID_SQL = "SELECT * FROM %s WHERE order_id = $1 LIMIT $2 OFFSET $3";

  private static final String INSERT_SQL = "INSERT INTO %s (id, action, order_id, user_id, user_name, event_date, action_date, modified_content_snapshot) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";

  @Autowired
  private final PostgresClientFactory pgClientFactory;

  @Autowired
  public OrderEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(OrderAuditEvent orderAuditEvent, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();

    String query = format(INSERT_SQL, TABLE_NAME);

    makeSaveCall(promise, query, orderAuditEvent, tenantId);

    return promise.future();
  }

  @Override
  public Future<OrderAuditEventCollection> getAcquisitionOrderAuditEventById(String orderId, int limit, int offset, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      String jobTable = formatDBTableName(tenantId, TABLE_NAME);
      String query = format(GET_BY_ID_SQL, jobTable);
      Tuple queryParams = Tuple.of(UUID.fromString(orderId), limit, offset);
      pgClientFactory.createInstance(tenantId).selectRead(query, queryParams, promise);
    } catch (Exception e) {
      LOGGER.error("Error getting OrderAuditEvent by id", e);
      promise.fail(e);
    }
    return promise.future().map(rowSet -> rowSet.rowCount() == 0 ? new OrderAuditEventCollection().withTotalItems(0)
      : mapRowToListOfOrderEvent(rowSet));

  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, OrderAuditEvent orderAuditEvent, String tenantId) {
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(orderAuditEvent.getId(),
        orderAuditEvent.getAction(),
        orderAuditEvent.getOrderId(),
        orderAuditEvent.getUserId(),
        orderAuditEvent.getUserName(),
        LocalDateTime.ofInstant(orderAuditEvent.getEventDate().toInstant(),ZoneId.systemDefault()),
        LocalDateTime.ofInstant(orderAuditEvent.getActionDate().toInstant(),ZoneId.systemDefault()),
        orderAuditEvent.getOrderSnapshot()), promise);
    } catch (Exception e) {
      LOGGER.error("Failed to save record with Id {} in to table {}", orderAuditEvent.getId(), TABLE_NAME, e);
      promise.fail(e);
    }
  }

  private OrderAuditEventCollection mapRowToListOfOrderEvent(RowSet<Row> rowSet) {
    OrderAuditEventCollection orderAuditEventCollection = new OrderAuditEventCollection();
    rowSet.iterator().forEachRemaining(row -> {
      orderAuditEventCollection.getOrderAuditEvents().add(mapRowToOrderEvent(row));
    });
    orderAuditEventCollection.setTotalItems(orderAuditEventCollection.getOrderAuditEvents().size());
    return orderAuditEventCollection;
 }

  private OrderAuditEvent mapRowToOrderEvent(Row row) {

    return new OrderAuditEvent()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(OrderAuditEvent.Action.class,ACTION_FIELD))
      .withOrderId(row.getValue(ORDER_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withOrderSnapshot(JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD)));
  }

  private String formatDBTableName(String tenantId, String table) {
    return format("%s.%s", convertToPsqlStandard(tenantId), table);
  }

}
