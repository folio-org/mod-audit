package org.folio.dao.acquisition.impl;

import static java.lang.String.format;
import static org.folio.util.AuditEventDBConstants.ACTION_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ID_FIELD;
import static org.folio.util.AuditEventDBConstants.INVOICE_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.MODIFIED_CONTENT_FIELD;
import static org.folio.util.AuditEventDBConstants.ORDER_BY_PATTERN;
import static org.folio.util.AuditEventDBConstants.TOTAL_RECORDS_FIELD;
import static org.folio.util.AuditEventDBConstants.USER_ID_FIELD;
import static org.folio.util.DbUtils.formatDBTableName;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.InvoiceEventsDao;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceEventsDaoImpl implements InvoiceEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_invoice_log";

  public static final String GET_BY_INVOICE_ID_SQL = "SELECT id, action, invoice_id, user_id, event_date, action_date, modified_content_snapshot," +
    " (SELECT count(*) AS total_records FROM %s WHERE invoice_id = $1) FROM %s WHERE invoice_id = $1 %s LIMIT $2 OFFSET $3";

  public static final String INSERT_SQL = "INSERT INTO %s (id, action, invoice_id, user_id, event_date, action_date, modified_content_snapshot)" +
    " VALUES ($1, $2, $3, $4, $5, $6, $7)";

  private final PostgresClientFactory pgClientFactory;

  public InvoiceEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(InvoiceAuditEvent event, String tenantId) {
    LOGGER.debug("save:: Saving Invoice AuditEvent with invoice id: {}", event.getInvoiceId());
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(INSERT_SQL, logTable);
    return makeSaveCall(query, event, tenantId)
      .onSuccess(rows -> LOGGER.info("save:: Saved Invoice AuditEvent with invoice id : {}", event.getInvoiceId()))
      .onFailure(e -> LOGGER.error("Failed to save record with id: {} for invoice id: {} in to table {}",
        event.getId(), event.getInvoiceId(), TABLE_NAME, e));
  }

  @Override
  public Future<InvoiceAuditEventCollection> getAuditEventsByInvoiceId(String invoiceId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByInvoiceId:: Retrieving AuditEvent with invoice id : {}", invoiceId);
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(GET_BY_INVOICE_ID_SQL, logTable, logTable,  format(ORDER_BY_PATTERN, sortBy, sortOrder));
    return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(UUID.fromString(invoiceId), limit, offset))
      .map(this::mapRowToListOfInvoiceEvent);
  }

  private Future<RowSet<Row>> makeSaveCall(String query, InvoiceAuditEvent invoiceAuditEvent, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(invoiceAuditEvent.getId(),
        invoiceAuditEvent.getAction(),
        invoiceAuditEvent.getInvoiceId(),
        invoiceAuditEvent.getUserId(),
        LocalDateTime.ofInstant(invoiceAuditEvent.getEventDate().toInstant(), ZoneId.systemDefault()),
        LocalDateTime.ofInstant(invoiceAuditEvent.getActionDate().toInstant(), ZoneId.systemDefault()),
        JsonObject.mapFrom(invoiceAuditEvent.getInvoiceSnapshot())));
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
  }

  private InvoiceAuditEventCollection mapRowToListOfInvoiceEvent(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToListOfInvoiceEvent:: Mapping row to List of Invoice Events");
    if (rowSet.rowCount() == 0) {
      return new InvoiceAuditEventCollection().withTotalItems(0);
    }
    InvoiceAuditEventCollection invoiceAuditEventCollection = new InvoiceAuditEventCollection();
    rowSet.iterator().forEachRemaining(row -> {
      invoiceAuditEventCollection.getInvoiceAuditEvents().add(mapRowToInvoiceEvent(row));
      invoiceAuditEventCollection.setTotalItems(row.getInteger(TOTAL_RECORDS_FIELD));
    });
    LOGGER.debug("mapRowToListOfInvoiceEvent:: Mapped row to List of Invoice Events");
    return invoiceAuditEventCollection;
  }

  private InvoiceAuditEvent mapRowToInvoiceEvent(Row row) {
    LOGGER.debug("mapRowToInvoiceEvent:: Mapping row to Invoice Event");
    return new InvoiceAuditEvent()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(InvoiceAuditEvent.Action.class, ACTION_FIELD))
      .withInvoiceId(row.getValue(INVOICE_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withInvoiceSnapshot(JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD)));
  }
}
