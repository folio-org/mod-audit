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
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static java.lang.String.format;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;
import static org.folio.util.OrderAuditEventDBConstants.*;

@Repository
public class OrderEventsDaoImpl implements OrderEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_order_log";

  public static final String GET_BY_ORDER_ID_SQL = "SELECT id, action, order_id, user_id, event_date, action_date, modified_content_snapshot," +
    " (SELECT count(*) AS total_records FROM %s WHERE order_id = $1) FROM %s WHERE order_id = $1 %s LIMIT $2 OFFSET $3";

  public static final String INSERT_SQL = "INSERT INTO %s (id, action, order_id, user_id, event_date, action_date, modified_content_snapshot)" +
    " VALUES ($1, $2, $3, $4, $5, $6, $7)";

  @Autowired
  private final PostgresClientFactory pgClientFactory;

  @Autowired
  public OrderEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(OrderAuditEvent orderAuditEvent, String tenantId) {
    LOGGER.debug("save:: Saving Order AuditEvent with tenant id : {}", tenantId);
    Promise<RowSet<Row>> promise = Promise.promise();
    String logTable = formatDBTableName(tenantId, TABLE_NAME);

    String query = format(INSERT_SQL, logTable);

    makeSaveCall(promise, query, orderAuditEvent, tenantId);
    LOGGER.info("save:: Saved Order AuditEvent with tenant id : {}", tenantId);
    return promise.future();
  }

  @Override
  public Future<OrderAuditEventCollection> getAuditEventsByOrderId(String orderId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderId:: Retrieving AuditEvent with order id : {}", orderId);
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      LOGGER.info("getAuditEventsByOrderId:: Trying to Retrieve AuditEvent with order id : {}", orderId);
      String logTable = formatDBTableName(tenantId, TABLE_NAME);
      String query = format(GET_BY_ORDER_ID_SQL, logTable, logTable,  format(ORDER_BY_PATTERN, sortBy, sortOrder));
      Tuple queryParams = Tuple.of(UUID.fromString(orderId), limit, offset);
      pgClientFactory.createInstance(tenantId).selectRead(query, queryParams, promise);
    } catch (Exception e) {
      LOGGER.warn("Error getting order audit events by order id: {}", orderId, e);
      promise.fail(e);
    }
    LOGGER.info("getAuditEventsByOrderId:: Retrieved AuditEvent with order id : {}", orderId);
    return promise.future().map(rowSet -> rowSet.rowCount() == 0 ? new OrderAuditEventCollection().withTotalItems(0)
      : mapRowToListOfOrderEvent(rowSet));
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, OrderAuditEvent orderAuditEvent, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      LOGGER.info("makeSaveCall:: Trying to make save call with query : {} and tenant id : {}", query, tenantId);
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(orderAuditEvent.getId(),
        orderAuditEvent.getAction(),
        orderAuditEvent.getOrderId(),
        orderAuditEvent.getUserId(),
        LocalDateTime.ofInstant(orderAuditEvent.getEventDate().toInstant(), ZoneOffset.UTC),
        LocalDateTime.ofInstant(orderAuditEvent.getActionDate().toInstant(), ZoneOffset.UTC),
        JsonObject.mapFrom(orderAuditEvent.getOrderSnapshot())), promise);
    } catch (Exception e) {
      LOGGER.warn("Failed to save record with id: {} for order id: {} in to table {}",
        orderAuditEvent.getId(), orderAuditEvent.getOrderId(), TABLE_NAME, e);
      promise.fail(e);
    }
  }

  private OrderAuditEventCollection mapRowToListOfOrderEvent(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToListOfOrderEvent:: Mapping row to List of Order Events");
    OrderAuditEventCollection orderAuditEventCollection = new OrderAuditEventCollection();
    rowSet.iterator().forEachRemaining(row -> {
      orderAuditEventCollection.getOrderAuditEvents().add(mapRowToOrderEvent(row));
      orderAuditEventCollection.setTotalItems(row.getInteger(TOTAL_RECORDS_FIELD));
    });
    LOGGER.info("mapRowToListOfOrderEvent:: Mapped row to List of Order Events");
    return orderAuditEventCollection;
  }

  private OrderAuditEvent mapRowToOrderEvent(Row row) {

    LOGGER.debug("mapRowToOrderEvent:: Mapping row to Order Event");
    return new OrderAuditEvent()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(OrderAuditEvent.Action.class, ACTION_FIELD))
      .withOrderId(row.getValue(ORDER_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withOrderSnapshot(JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD)));
  }

  private String formatDBTableName(String tenantId, String table) {
    LOGGER.debug("formatDBTableName:: Formatting DB Table Name with tenant id : {}", tenantId);
    return format("%s.%s", convertToPsqlStandard(tenantId), table);
  }
}
