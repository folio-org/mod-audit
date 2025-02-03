package org.folio.dao.acquisition.impl;

import static java.lang.String.format;
import static org.folio.util.AuditEventDBConstants.ACTION_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ID_FIELD;
import static org.folio.util.AuditEventDBConstants.MODIFIED_CONTENT_FIELD;
import static org.folio.util.AuditEventDBConstants.ORDER_BY_PATTERN;
import static org.folio.util.AuditEventDBConstants.PIECE_ID_FIELD;
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
import org.folio.dao.acquisition.PieceEventsDao;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PieceEventsDaoImpl implements PieceEventsDao {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final String TABLE_NAME = "acquisition_piece_log";
  private static final String GET_BY_PIECE_ID_SQL = "SELECT id, action, piece_id, user_id, event_date, action_date, modified_content_snapshot,  " +
    " (SELECT count(*) AS total_records FROM %s WHERE piece_id = $1) FROM %s WHERE piece_id = $1 %s LIMIT $2 OFFSET $3";
  private static final String GET_STATUS_CHANGE_HISTORY_BY_PIECE_ID_SQL = """
    WITH StatusChanges AS (
      SELECT id, action, piece_id, user_id, event_date, action_date, modified_content_snapshot,
        LAG(modified_content_snapshot ->> 'receivingStatus') OVER w AS previous_status,
        LAG(modified_content_snapshot ->> 'claimingInterval') OVER w AS previous_claiming_interval
      FROM %s WHERE piece_id=$1
      WINDOW w AS (PARTITION BY piece_id ORDER BY action_date)
    )
    SELECT id, action, piece_id, user_id, event_date, action_date, modified_content_snapshot,
      (SELECT COUNT(*) AS total_records FROM StatusChanges
        WHERE modified_content_snapshot ->> 'receivingStatus' IS DISTINCT FROM previous_status
          OR modified_content_snapshot ->> 'claimingInterval' IS DISTINCT FROM previous_claiming_interval)
    FROM StatusChanges
    WHERE modified_content_snapshot ->> 'receivingStatus' IS DISTINCT FROM previous_status
      OR modified_content_snapshot ->> 'claimingInterval' IS DISTINCT FROM previous_claiming_interval
    %s LIMIT $2 OFFSET $3
    """;

  private static final String INSERT_SQL = "INSERT INTO %s (id, action, piece_id, user_id, event_date, action_date, modified_content_snapshot)" +
    " VALUES ($1, $2, $3, $4, $5, $6, $7)";

  private final PostgresClientFactory pgClientFactory;

  public PieceEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(PieceAuditEvent event, String tenantId) {
    LOGGER.debug("save:: Trying to save Piece AuditEvent with piece id : {}", event.getPieceId());
    Promise<RowSet<Row>> promise = Promise.promise();
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(INSERT_SQL, logTable);
    makeSaveCall(promise, query, event, tenantId);
    LOGGER.info("save:: Saved Piece AuditEvent for pieceId={} in tenant id={}", event.getPieceId(), tenantId);
    return promise.future();
  }

  @Override
  public Future<PieceAuditEventCollection> getAuditEventsByPieceId(String pieceId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderId:: Trying to retrieve AuditEvent with piece id : {}", pieceId);
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      LOGGER.debug("formatDBTableName:: Formatting DB Table Name with tenant id : {}", tenantId);
      String logTable = formatDBTableName(tenantId, TABLE_NAME);
      String query = format(GET_BY_PIECE_ID_SQL, logTable, logTable, format(ORDER_BY_PATTERN, sortBy, sortOrder));
      Tuple queryParams = Tuple.of(UUID.fromString(pieceId), limit, offset);
      pgClientFactory.createInstance(tenantId).selectRead(query, queryParams, promise);
    } catch (Exception e) {
      LOGGER.warn("Error getting piece audit events by piece id: {}", pieceId, e);
      promise.fail(e);
    }
    LOGGER.info("getAuditEventsByOrderId:: Retrieved AuditEvent with piece id : {}", pieceId);
    return promise.future().map(this::mapRowToListOfPieceEvent);
  }

  @Override
  public Future<PieceAuditEventCollection> getAuditEventsWithStatusChangesByPieceId(String pieceId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderId:: Retrieving AuditEvent with piece id : {}", pieceId);
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      LOGGER.debug("formatDBTableName:: Formatting DB Table Name with tenant id : {}", tenantId);
      String logTable = formatDBTableName(tenantId, TABLE_NAME);
      String query = format(GET_STATUS_CHANGE_HISTORY_BY_PIECE_ID_SQL, logTable, format(ORDER_BY_PATTERN, sortBy, sortOrder));
      Tuple queryParams = Tuple.of(UUID.fromString(pieceId), limit, offset);
      pgClientFactory.createInstance(tenantId).selectRead(query, queryParams, promise);
    } catch (Exception e) {
      LOGGER.warn("Error getting order audit events by piece id: {}", pieceId, e);
      promise.fail(e);
    }
    LOGGER.info("getAuditEventsByOrderId:: Retrieved AuditEvent with piece id: {}", pieceId);
    return promise.future().map(this::mapRowToListOfPieceEvent);
  }

  private PieceAuditEventCollection mapRowToListOfPieceEvent(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToListOfOrderEvent:: Mapping row to List of Piece Events");
    if (rowSet.rowCount() == 0) {
      return new PieceAuditEventCollection().withTotalItems(0);
    }
    PieceAuditEventCollection pieceAuditEventCollection = new PieceAuditEventCollection();
    rowSet.iterator().forEachRemaining(row -> {
      pieceAuditEventCollection.getPieceAuditEvents().add(mapRowToPieceEvent(row));
      pieceAuditEventCollection.setTotalItems(row.getInteger(TOTAL_RECORDS_FIELD));
    });
    LOGGER.debug("mapRowToListOfOrderEvent:: Mapped row to List of Piece Events");
    return pieceAuditEventCollection;
  }

  private PieceAuditEvent mapRowToPieceEvent(Row row) {
    LOGGER.debug("mapRowToPieceEvent:: Mapping row to Order Event");
    return new PieceAuditEvent()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(PieceAuditEvent.Action.class, ACTION_FIELD))
      .withPieceId(row.getValue(PIECE_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withPieceSnapshot(JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD)));
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, PieceAuditEvent pieceAuditEvent, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(pieceAuditEvent.getId(),
          pieceAuditEvent.getAction(),
          pieceAuditEvent.getPieceId(),
          pieceAuditEvent.getUserId(),
          LocalDateTime.ofInstant(pieceAuditEvent.getEventDate().toInstant(),  ZoneId.systemDefault()),
          LocalDateTime.ofInstant(pieceAuditEvent.getActionDate().toInstant(), ZoneId.systemDefault()),
          JsonObject.mapFrom(pieceAuditEvent.getPieceSnapshot())),
        promise);
    } catch (Exception e) {
      LOGGER.error("Failed to save record with id: {} for order id: {} in to table {}",
        pieceAuditEvent.getId(), pieceAuditEvent.getPieceId(), TABLE_NAME, e);
      promise.fail(e);
    }
  }
}
