package org.folio.dao.acquisition.impl;

import static java.lang.String.format;
import static org.folio.util.AuditEventDBConstants.ACTION_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ID_FIELD;
import static org.folio.util.AuditEventDBConstants.MODIFIED_CONTENT_FIELD;
import static org.folio.util.AuditEventDBConstants.ORDER_BY_PATTERN;
import static org.folio.util.AuditEventDBConstants.ORDER_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.ORDER_LINE_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.TOTAL_RECORDS_FIELD;
import static org.folio.util.AuditEventDBConstants.USER_ID_FIELD;
import static org.folio.util.DbUtils.formatDBTableName;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

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
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class OrderLineEventsDaoImpl implements OrderLineEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_order_line_log";

  public static final String GET_BY_ORDER_LINE_ID_SQL = "SELECT id, action, order_id, order_line_id, user_id, event_date, action_date, modified_content_snapshot," +
    " (SELECT count(*) AS total_records FROM %s WHERE order_line_id = $1) " +
    " FROM %s WHERE order_line_id = $1 %s LIMIT $2 OFFSET $3";

  private static final String INSERT_SQL = "INSERT INTO %s (id, action, order_id, order_line_id, user_id, event_date, action_date, modified_content_snapshot) " +
    "VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";

  private final PostgresClientFactory pgClientFactory;

  public OrderLineEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(OrderLineAuditEvent event, String tenantId) {
    LOGGER.debug("save:: Saving OrderLine AuditEvent with order line id : {}", event.getOrderLineId());
    Promise<RowSet<Row>> promise = Promise.promise();
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(INSERT_SQL, logTable);
    makeSaveCall(promise, query, event, tenantId);
    LOGGER.info("save:: Saved OrderLine AuditEvent with order line id : {}", event.getOrderLineId());
    return promise.future();
  }

  @Override
  public Future<OrderLineAuditEventCollection> getAuditEventsByOrderLineId(String orderLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderLineId:: Retrieving AuditEvent with order line id : {}", orderLineId);
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      LOGGER.debug("formatDBTableName:: Formatting DB Table Name with tenant id : {}", tenantId);
      String logTable = formatDBTableName(tenantId, TABLE_NAME);
      String query = format(GET_BY_ORDER_LINE_ID_SQL, logTable, logTable, format(ORDER_BY_PATTERN, sortBy, sortOrder));
      Tuple queryParams = Tuple.of(UUID.fromString(orderLineId), limit, offset);
      pgClientFactory.createInstance(tenantId).selectRead(query, queryParams, promise);
    } catch (Exception e) {
      LOGGER.warn("Error getting order line audit events by order line id: {}", orderLineId, e);
      promise.fail(e);
    }

    LOGGER.info("getAuditEventsByOrderLineId:: Retrieved AuditEvent with order line id : {}", orderLineId);
    return promise.future().map(this::mapRowToListOfOrderLineEvent);
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, OrderLineAuditEvent orderLineAuditEvent, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(orderLineAuditEvent.getId(),
        orderLineAuditEvent.getAction(),
        orderLineAuditEvent.getOrderId(),
        orderLineAuditEvent.getOrderLineId(),
        orderLineAuditEvent.getUserId(),
        LocalDateTime.ofInstant(orderLineAuditEvent.getEventDate().toInstant(), ZoneId.systemDefault()),
        LocalDateTime.ofInstant(orderLineAuditEvent.getActionDate().toInstant(), ZoneId.systemDefault()),
        JsonObject.mapFrom(orderLineAuditEvent.getOrderLineSnapshot())), promise);
    } catch (Exception e) {
      LOGGER.error("Failed to save record with id: {} for order line id: {} in to table {}",
        orderLineAuditEvent.getId(), orderLineAuditEvent.getOrderLineId(), TABLE_NAME, e);
      promise.fail(e);
    }
  }

  private OrderLineAuditEventCollection mapRowToListOfOrderLineEvent(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToListOfOrderLineEvent:: Mapping row to List of Order Line Events");
    if (rowSet.rowCount() == 0) {
      return new OrderLineAuditEventCollection().withTotalItems(0);
    }
    OrderLineAuditEventCollection orderLineAuditEventCollection = new OrderLineAuditEventCollection();
    rowSet.iterator().forEachRemaining(row -> {
      orderLineAuditEventCollection.getOrderLineAuditEvents().add(mapRowToOrderLineEvent(row));
      orderLineAuditEventCollection.setTotalItems(row.getInteger(TOTAL_RECORDS_FIELD));
    });
    LOGGER.debug("mapRowToListOfOrderLineEvent:: Mapped row to List of Order Line Events");
    return orderLineAuditEventCollection;
  }

  private OrderLineAuditEvent mapRowToOrderLineEvent(Row row) {

    LOGGER.debug("mapRowToOrderLineEvent:: Mapping row to Order Line Event");
    return new OrderLineAuditEvent()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(OrderLineAuditEvent.Action.class, ACTION_FIELD))
      .withOrderId(row.getValue(ORDER_ID_FIELD).toString())
      .withOrderLineId(row.getValue(ORDER_LINE_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withOrderLineSnapshot(JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD)));
  }
}
