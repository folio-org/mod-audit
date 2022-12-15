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
import org.folio.rest.jaxrs.model.OrderAuditEventDto;
import org.folio.rest.jaxrs.model.OrderSnapshot;
import org.folio.util.PostgresClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.*;

import static java.lang.String.format;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;
import static org.folio.util.OrderAuditEventDBConstants.*;

@Repository
public class OrderEventsDaoImpl implements OrderEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_order_log";

  public static final String GET_BY_ID_SQL = "SELECT * FROM %s WHERE id = $1";

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
  public Future<Optional<OrderAuditEventDto>> getAcquisitionOrderAuditEventById(String id, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      String jobTable = formatDBTableName(tenantId, TABLE_NAME);
      String query = format(GET_BY_ID_SQL, jobTable);
      pgClientFactory.createInstance(tenantId).selectRead(query, Tuple.of(UUID.fromString(id)), promise);
    } catch (Exception e) {
      LOGGER.error("Error getting OrderAuditEvent by id", e);
      promise.fail(e);
    }
    return promise.future().map(rowSet ->  rowSet.rowCount() == 0 ? Optional.empty()
      : Optional.of(mapRowToOrderEvent(rowSet.iterator().next())));

  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, OrderAuditEvent orderAuditEvent, String tenantId) {
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(orderAuditEvent.getId(),
        orderAuditEvent.getAction(),
        orderAuditEvent.getOrderId(),
        orderAuditEvent.getUserId(),
        orderAuditEvent.getUserName(),
        orderAuditEvent.getEventDate(),
        orderAuditEvent.getActionDate(),
        new JsonObject(orderAuditEvent.getOrderSnapshot())), promise);
    } catch (Exception e) {
      LOGGER.error("Failed to save record with Id {} in to table {}", orderAuditEvent.getId(), TABLE_NAME, e);
      promise.fail(e);
    }
  }

  private OrderAuditEventDto mapRowToOrderEvent(Row row) {
    return new OrderAuditEventDto()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(OrderAuditEventDto.Action.class,ACTION_FIELD))
      .withOrderId(row.getValue(ORDER_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withOrderSnapshot(mapToOrderSnapshot(row));
 }

  private String formatDBTableName(String tenantId, String table) {
    return format("%s.%s", convertToPsqlStandard(tenantId), table);
  }

  private OrderSnapshot mapToOrderSnapshot(Row row) {
    OrderSnapshot orderSnapshot = new OrderSnapshot();
    JsonObject jsonObject = JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD));
    jsonObject.stream().iterator().forEachRemaining(ar->orderSnapshot.withAdditionalProperty(ar.getKey(),ar.getValue()));

    return orderSnapshot;
  }

}
