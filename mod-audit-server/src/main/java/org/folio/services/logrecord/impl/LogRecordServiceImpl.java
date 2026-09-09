package org.folio.services.logrecord.impl;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.rest.impl.CirculationLogsService.LOGS_TABLE_NAME;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.builder.LogRecordBuilderResolver;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.logrecord.LogRecordService;
import org.folio.util.PostgresClientFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LogRecordServiceImpl implements LogRecordService {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final String SEARCH_BY_LOAN_ID_QUERY_PATTERN = "items=@loanId %s";

  private final PostgresClientFactory pgClientFactory;

  public LogRecordServiceImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<Void> processLogRecord(String logEventType, JsonObject payload, Map<String, String> okapiHeaders,
    Context vertxContext) {
    LOGGER.debug("processLogRecord:: Processing log record for logEventType: {}", logEventType);
    var builder = LogRecordBuilderResolver.getBuilder(logEventType, okapiHeaders, vertxContext);
    var tenantId = TenantTool.tenantId(okapiHeaders);

    return Future.fromCompletionStage(builder.buildLogRecord(payload))
      .compose(logRecords -> processAnonymize(logRecords, tenantId))
      .compose(logRecords -> saveLogRecords(logRecords, tenantId));
  }

  private Future<List<LogRecord>> processAnonymize(List<LogRecord> records, String tenantId) {
    LOGGER.debug("processAnonymize:: Processing anonymize for records");
    return isAnonymize(records) ?
      anonymizeLoanRelatedRecords(records, tenantId) :
      Future.succeededFuture(records);
  }

  private boolean isAnonymize(List<LogRecord> records) {
    LOGGER.debug("isAnonymize:: Checking if anonymize is required for records");
    return records.stream()
      .anyMatch(logRecord -> LogRecord.Action.ANONYMIZE == logRecord.getAction());
  }

  private Future<List<LogRecord>> anonymizeLoanRelatedRecords(List<LogRecord> records, String tenantId) {
    LOGGER.debug("anonymizeLoanRelatedRecords:: Anonymize loan-related records for log records");
    List<LogRecord> result = new ArrayList<>();
    if (isEmpty(records) || isEmpty(records.getFirst().getItems())) {
      return Future.succeededFuture(result);
    }

    var firstLogRecord = records.getFirst();
    result.add(firstLogRecord);
    var query = SEARCH_BY_LOAN_ID_QUERY_PATTERN.formatted(firstLogRecord.getItems().getFirst().getLoanId());

    CQLWrapper cqlWrapper;
    try {
      cqlWrapper = buildCqlWrapper(query, Integer.MAX_VALUE, 0);
    } catch (Exception e) {
      LOGGER.warn("Failed to build CQL wrapper for anonymize loan-related records due to : {}", e.getMessage());
      return Future.failedFuture(e);
    }

    Promise<List<LogRecord>> promise = Promise.promise();
    pgClientFactory.createInstance(tenantId)
      .get(LOGS_TABLE_NAME, LogRecord.class, new String[] { "*" }, cqlWrapper, false, false, reply -> {
        if (reply.succeeded()) {
          LOGGER.info("anonymizeLoanRelatedRecords:: Anonymize loan-related records for log records Successfully");
          reply.result().getResults().forEach(logRecord -> {
            logRecord.setUserBarcode(null);
            logRecord.setLinkToIds(logRecord.getLinkToIds().withUserId(null));
            result.add(logRecord);
          });
          promise.complete(result);
        } else {
          LOGGER.warn("Failed Anonymize loan-related records for log records due to : {}", reply.cause().getMessage());
          promise.fail(reply.cause());
        }
      });
    return promise.future();
  }

  private Future<Void> saveLogRecords(List<LogRecord> logRecords, String tenantId) {
    LOGGER.debug("saveLogRecords:: Saving log records");
    return pgClientFactory.createInstance(tenantId).upsertBatch(LOGS_TABLE_NAME, logRecords)
      .onFailure(cause -> LOGGER.warn("Error saving log records: {}", cause.getMessage()))
      .mapEmpty();
  }

  private CQLWrapper buildCqlWrapper(String query, int limit, int offset) throws Exception {
    LOGGER.debug("buildCqlWrapper:: Creating CQL wrapper");
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(LOGS_TABLE_NAME + ".jsonb");
    return new CQLWrapper(cql2PgJSON, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
  }
}
