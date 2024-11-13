package org.folio.dao.acquisition.impl;

import static java.lang.String.format;
import static org.folio.util.AuditEventDBConstants.ACTION_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.ID_FIELD;
import static org.folio.util.AuditEventDBConstants.INVOICE_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.INVOICE_LINE_ID_FIELD;
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
import org.folio.dao.acquisition.InvoiceLineEventsDao;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEventCollection;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceLineEventsDaoImpl implements InvoiceLineEventsDao {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TABLE_NAME = "acquisition_invoice_line_log";

  public static final String GET_BY_INVOICE_LINE_ID_SQL = "SELECT id, action, invoice_id, invoice_line_id, user_id, event_date, action_date, modified_content_snapshot," +
    " (SELECT count(*) AS total_records FROM %s WHERE invoice_line_id = $1) " +
    " FROM %s WHERE invoice_line_id = $1 %s LIMIT $2 OFFSET $3";

  private static final String INSERT_SQL = "INSERT INTO %s (id, action, invoice_id, invoice_line_id, user_id, event_date, action_date, modified_content_snapshot) " +
    "VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";

  private final PostgresClientFactory pgClientFactory;

  public InvoiceLineEventsDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(InvoiceLineAuditEvent event, String tenantId) {
    LOGGER.debug("save:: Saving InvoiceLine AuditEvent with invoice line id: {}", event.getInvoiceLineId());
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(INSERT_SQL, logTable);
    return makeSaveCall(query, event, tenantId)
      .onSuccess(rows -> LOGGER.info("save:: Saved InvoiceLine AuditEvent with invoice line id : {}", event.getInvoiceLineId()))
      .onFailure(e -> LOGGER.error("Failed to save record with id: {} for invoice line id: {} in to table {}",
        event.getId(), event.getInvoiceLineId(), TABLE_NAME, e));
  }

  @Override
  public Future<InvoiceLineAuditEventCollection> getAuditEventsByInvoiceLineId(String invoiceLineId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByInvoiceLineId:: Retrieving AuditEvent with invoice line id : {}", invoiceLineId);
    String logTable = formatDBTableName(tenantId, TABLE_NAME);
    String query = format(GET_BY_INVOICE_LINE_ID_SQL, logTable, logTable, format(ORDER_BY_PATTERN, sortBy, sortOrder));
    return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(UUID.fromString(invoiceLineId), limit, offset))
      .map(this::mapRowToListOfInvoiceLineEvent);
  }

  private Future<RowSet<Row>> makeSaveCall(String query, InvoiceLineAuditEvent invoiceLineAuditEvent, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(invoiceLineAuditEvent.getId(),
        invoiceLineAuditEvent.getAction(),
        invoiceLineAuditEvent.getInvoiceId(),
        invoiceLineAuditEvent.getInvoiceLineId(),
        invoiceLineAuditEvent.getUserId(),
        LocalDateTime.ofInstant(invoiceLineAuditEvent.getEventDate().toInstant(), ZoneId.systemDefault()),
        LocalDateTime.ofInstant(invoiceLineAuditEvent.getActionDate().toInstant(), ZoneId.systemDefault()),
        JsonObject.mapFrom(invoiceLineAuditEvent.getInvoiceLineSnapshot())));
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
  }

  private InvoiceLineAuditEventCollection mapRowToListOfInvoiceLineEvent(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToListOfInvoiceLineEvent:: Mapping row to List of Invoice Line Events");
    if (rowSet.rowCount() == 0) {
      return new InvoiceLineAuditEventCollection().withTotalItems(0);
    }
    InvoiceLineAuditEventCollection invoiceLineAuditEventCollection = new InvoiceLineAuditEventCollection();
    rowSet.iterator().forEachRemaining(row -> {
      invoiceLineAuditEventCollection.getInvoiceLineAuditEvents().add(mapRowToInvoiceLineEvent(row));
      invoiceLineAuditEventCollection.setTotalItems(row.getInteger(TOTAL_RECORDS_FIELD));
    });
    LOGGER.debug("mapRowToListOfInvoiceLineEvent:: Mapped row to List of Invoice Line Events");
    return invoiceLineAuditEventCollection;
  }

  private InvoiceLineAuditEvent mapRowToInvoiceLineEvent(Row row) {
    LOGGER.debug("mapRowToInvoiceLineEvent:: Mapping row to Invoice Line Event");
    return new InvoiceLineAuditEvent()
      .withId(row.getValue(ID_FIELD).toString())
      .withAction(row.get(InvoiceLineAuditEvent.Action.class, ACTION_FIELD))
      .withInvoiceId(row.getValue(INVOICE_ID_FIELD).toString())
      .withInvoiceLineId(row.getValue(INVOICE_LINE_ID_FIELD).toString())
      .withUserId(row.getValue(USER_ID_FIELD).toString())
      .withEventDate(Date.from(row.getLocalDateTime(EVENT_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withActionDate(Date.from(row.getLocalDateTime(ACTION_DATE_FIELD).toInstant(ZoneOffset.UTC)))
      .withInvoiceLineSnapshot(JsonObject.mapFrom(row.getValue(MODIFIED_CONTENT_FIELD)));
  }
}
