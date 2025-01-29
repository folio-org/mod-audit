package org.folio.dao.inventory.impl;

import static java.lang.String.format;
import static org.folio.util.DbUtils.formatDBTableName;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.dao.inventory.InventoryEventDao;
import org.folio.util.PostgresClientFactory;

public abstract class InventoryEventDaoImpl implements InventoryEventDao {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final String INVENTORY_AUDIT_TABLE = "%s_audit";

  private static final String INSERT_SQL = """
    INSERT INTO %s (event_id, event_date, entity_id, action, user_id, diff)
    VALUES ($1, $2, $3, $4, $5, $6)
    """;

  private final PostgresClientFactory pgClientFactory;

  protected InventoryEventDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(InventoryAuditEntity event, String tenantId) {
    LOGGER.debug("save:: Trying to save InventoryAuditEntity with [tenantId: {}, eventId:{}, entityId:{}]",
      tenantId, event.eventId(), event.entityId());
    var promise = Promise.<RowSet<Row>>promise();
    var table = formatDBTableName(tenantId, tableName());
    var query = format(INSERT_SQL, table);
    makeSaveCall(promise, query, event, tenantId);
    return promise.future();
  }

  private String tableName() {
    return INVENTORY_AUDIT_TABLE.formatted(resourceType().getType());
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, InventoryAuditEntity event, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(event.eventId(),
          LocalDateTime.ofInstant(event.eventDate().toInstant(),  ZoneId.systemDefault()),
          event.entityId(),
          event.action(),
          event.userId(),
          JsonObject.mapFrom(event.diff())),
        promise);
      LOGGER.info("save:: Saved InventoryAuditEntity with [tenantId: {}, eventId:{}, entityId:{}]",
        tenantId, event.eventId(), event.entityId());
    } catch (Exception e) {
      LOGGER.error("Failed to save record with [eventId:{}, entityId:{}, tableName: {}]",
        event.eventId(), event.entityId(), tableName(), e);
      promise.fail(e);
    }
  }
}
