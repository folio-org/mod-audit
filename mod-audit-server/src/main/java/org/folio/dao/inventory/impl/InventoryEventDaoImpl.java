package org.folio.dao.inventory.impl;

import static java.lang.String.format;
import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.DIFF_FIELD;
import static org.folio.util.AuditEventDBConstants.ENTITY_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.USER_ID_FIELD;
import static org.folio.util.DbUtils.formatDBTableName;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
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

  private static final String SELECT_SQL = """
    SELECT * FROM %s
      WHERE entity_id = $1 %s
      ORDER BY event_date DESC
      LIMIT $2
    """;

  private static final String SEEK_BY_DATE_CLAUSE = "AND event_date < '%s'";

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

  @Override
  public Future<List<InventoryAuditEntity>> get(String tenantId, UUID entityId, LocalDateTime eventDate, int limit) {
    LOGGER.debug("get:: Retrieve records by [tenantId: {}, eventId:{}, eventDate before:{}, limit: {}]",
      tenantId, entityId, eventDate, limit);
    var table = formatDBTableName(tenantId, tableName());
    var seekByDateClause = eventDate == null ? "" : SEEK_BY_DATE_CLAUSE.formatted(eventDate);
    var query = SELECT_SQL.formatted(table, seekByDateClause);
    return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(entityId, limit))
      .map(this::mapRowToInventoryAuditEntityList);
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
      LOGGER.info("makeSaveCall:: Saved InventoryAuditEntity with [tenantId: {}, eventId:{}, entityId:{}]",
        tenantId, event.eventId(), event.entityId());
    } catch (Exception e) {
      LOGGER.error("Failed to save record with [eventId:{}, entityId:{}, tableName: {}]",
        event.eventId(), event.entityId(), tableName(), e);
      promise.fail(e);
    }
  }

  private List<InventoryAuditEntity> mapRowToInventoryAuditEntityList(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToInventoryAuditEntityList:: Mapping row set to List of Inventory Audit Entities");
    if (rowSet.rowCount() == 0) {
      return new LinkedList<>();
    }
    var entities = new LinkedList<InventoryAuditEntity>();
    rowSet.iterator().forEachRemaining(row ->
      entities.add(mapRowToInventoryAuditEntity(row)));
    LOGGER.debug("mapRowToInventoryAuditEntityList:: Mapped row set to List of Inventory Audit Entities");
    return entities;
  }

  private InventoryAuditEntity mapRowToInventoryAuditEntity(Row row) {
    LOGGER.debug("mapRowToInventoryAuditEntity:: Mapping row to Inventory Audit Entity");
    var diffJson = row.getJsonObject(DIFF_FIELD);
    return new InventoryAuditEntity(
      row.getUUID(EVENT_ID_FIELD),
      Timestamp.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)),
      row.getUUID(ENTITY_ID_FIELD),
      row.getString(ACTION_FIELD),
      row.getUUID(USER_ID_FIELD),
      diffJson == null ? null : diffJson.getMap()
    );
  }
}
