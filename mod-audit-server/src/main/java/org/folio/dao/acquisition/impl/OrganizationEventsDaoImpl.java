package org.folio.dao.acquisition.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrganizationEventsDao;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.jaxrs.model.OrganizationAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static java.lang.String.format;
import static org.folio.util.AuditEventDBConstants.ACTION_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ID_FIELD;
import static org.folio.util.AuditEventDBConstants.MODIFIED_CONTENT_FIELD;
import static org.folio.util.AuditEventDBConstants.ORDER_BY_PATTERN;
import static org.folio.util.AuditEventDBConstants.ORGANIZATION_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.TOTAL_RECORDS_FIELD;
import static org.folio.util.AuditEventDBConstants.USER_ID_FIELD;
import static org.folio.util.DbUtils.formatDBTableName;

@Repository
public class OrganizationEventsDaoImpl implements OrganizationEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_organization_log";

  public static final String GET_BY_INVOICE_ID_SQL = "SELECT id, action, organization_id, user_id, event_date, action_date, modified_content_snapshot," +
    " (SELECT count(*) AS total_records FROM %s WHERE organization_id = $1) FROM %s WHERE organization_id = $1 %s LIMIT $2 OFFSET $3";

  public static final String INSERT_SQL = "INSERT INTO %s (id, action, organization_id, user_id, event_date, action_date, modified_content_snapshot)" +
    " VALUES ($1, $2, $3, $4, $5, $6, $7)";

  private final PostgresClientFactory pgClientFactory;

  public OrganizationEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(OrganizationAuditEvent event, String tenantId) {
    LOGGER.debug("save:: Saving Organization AuditEvent with organization id : {}", event.getOrganizationId());
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(INSERT_SQL, logTable);
    return makeSaveCall(query, event, tenantId)
      .onSuccess(rows -> LOGGER.info("save:: Saved Organization AuditEvent with organization id: {}", event.getOrganizationId()))
      .onFailure(e -> LOGGER.error("Failed to save record with id: {} for organization id: {} in to table {}",
        event.getId(), event.getOrganizationId(), TABLE_NAME, e));
  }

  @Override
  public Future<OrganizationAuditEventCollection> getAuditEventsByOrganizationId(String organizationId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrganizationId:: Retrieving AuditEvent with organization id : {}", organizationId);
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(GET_BY_INVOICE_ID_SQL, logTable, logTable,  format(ORDER_BY_PATTERN, sortBy, sortOrder));
    return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(UUID.fromString(organizationId), limit, offset))
      .map(this::mapRowToListOfOrganizationEvent);
  }

  private Future<RowSet<Row>> makeSaveCall(String query, OrganizationAuditEvent organizationAuditEvent, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(organizationAuditEvent.getId(),
        organizationAuditEvent.getAction(),
        organizationAuditEvent.getOrganizationId(),
        organizationAuditEvent.getUserId(),
        LocalDateTime.ofInstant(organizationAuditEvent.getEventDate().toInstant(), ZoneId.systemDefault()),
        LocalDateTime.ofInstant(organizationAuditEvent.getActionDate().toInstant(), ZoneId.systemDefault()),
        JsonObject.mapFrom(organizationAuditEvent.getOrganizationSnapshot())));
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
  }

  private OrganizationAuditEventCollection mapRowToListOfOrganizationEvent(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToListOfOrganizationEvent:: Mapping row to List of Organization Events");
    if (rowSet.rowCount() == 0) {
      return new OrganizationAuditEventCollection().withTotalItems(0);
    }
    OrganizationAuditEventCollection organizationAuditEventCollection = new OrganizationAuditEventCollection();
    rowSet.iterator().forEachRemaining(row -> {
      organizationAuditEventCollection.getOrganizationAuditEvents().add(mapRowToOrganizationEvent(row));
      organizationAuditEventCollection.setTotalItems(row.getInteger(TOTAL_RECORDS_FIELD));
    });
    LOGGER.debug("mapRowToListOfOrganizationEvent:: Mapped row to List of Organization Events");
    return organizationAuditEventCollection;
  }

  private OrganizationAuditEvent mapRowToOrganizationEvent(Row row) {
    LOGGER.debug("mapRowToOrganizationEvent:: Mapping row to Organization Event");
    return new OrganizationAuditEvent()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(OrganizationAuditEvent.Action.class, ACTION_FIELD))
      .withOrganizationId(row.getValue(ORGANIZATION_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withOrganizationSnapshot(JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD)));
  }
}
