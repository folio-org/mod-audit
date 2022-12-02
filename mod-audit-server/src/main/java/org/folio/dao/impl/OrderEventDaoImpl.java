package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.OrderEvenDao;
import org.folio.util.PostgresClientFactory;

import java.util.Date;

import static java.lang.String.format;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

public class OrderEventDaoImpl implements OrderEvenDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_order_log";

  private static final String INSERT_SQL = "INSERT INTO %s.%s.%s.%s.%s.%s.%s (id, action, order_id, user_id, event_date, action_date, modified_content_snapshot) VALUES ($1, $2, $3, 4$, 5$, 6$, 7$)";

  private final PostgresClientFactory pgClientFactory;

  public OrderEventDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(String id, String action, String orderId, String userId,
                                  Date eventDate, Date action_date, String modifiedContent, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    String query = format(INSERT_SQL, convertToPsqlStandard(tenantId), TABLE_NAME);

    makeSaveCall(promise, query, id, action, orderId, userId, eventDate, action_date,
      modifiedContent, tenantId);

    return promise.future();
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, String id, String action, String orderId, String userId,
                            Date eventDate, Date action_date, String modifiedContent, String tenantId) {
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(id, action, orderId, userId,
        eventDate, action_date, modifiedContent, tenantId), promise);
    } catch (Exception e) {
      LOGGER.error("Failed to save record with Id {} in to table {}", id, TABLE_NAME, e);
      promise.fail(e);
    }
  }
}
