package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.isNull;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.Constants.NO_BARCODE;
import static org.folio.util.ErrorUtils.buildError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.folio.rest.jaxrs.resource.AuditDataCirculation;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.interfaces.Results;
import org.z3950.zing.cql.CQLDefaultNodeVisitor;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.ModifierSet;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class CirculationLogsService extends BaseService implements AuditDataCirculation {
  private static final Logger LOGGER = LogManager.getLogger();
  public static final String LOGS_TABLE_NAME = "circulation_logs";

  @Override
  @Validate
  public void getAuditDataCirculationLogs(String query, int offset, int limit, String lang,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataCirculationLogs:: Getting audit data circulation logs");

    CompletableFuture<Results<LogRecord>> logRecordResultFuture = createCqlWrapper(LOGS_TABLE_NAME, query, limit, offset)
      .thenCompose(cqlWrapper -> getClient(okapiHeaders, vertxContext)
        .withReadTrans(conn -> disableIndexScanIfNeeded(conn, query)
          .compose(x -> conn.get(LOGS_TABLE_NAME, LogRecord.class, cqlWrapper, true)))
        .toCompletionStage());

    logRecordResultFuture.thenAccept(result -> {
      LOGGER.info("getAuditDataCirculationLogs:: Successfully retrieved audit data circulation logs");

      List<LogRecord> logRecordList = result.getResults();
      logRecordList.stream().filter(logRecord -> isNull(logRecord.getUserBarcode()))
        .forEach(logRecord -> logRecord.setUserBarcode(NO_BARCODE));

      asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
        .respond200WithApplicationJson(new LogRecordCollection()
          .withLogRecords(logRecordList)
          .withTotalRecords(result.getResultInfo().getTotalRecords()))));
    }).exceptionally(throwable -> {
      LOGGER.warn("Exception occurred while getting audit data circulation logs: {}", throwable.getLocalizedMessage());
      asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse.
        respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), throwable.getLocalizedMessage()))));
      return null;
    });
  }

  /**
   * Disables index scan if needed based on CQL query validation.
   *
   * @param conn  The database connection
   * @param query The CQL query to validate
   * @return A Future that completes after setting the index scan configuration
   */
  private Future<RowSet<Row>> disableIndexScanIfNeeded(Conn conn, String query) {
    Boolean needsDisableIndexScan = validateCqlForDisableIndexScan(query);
    if (Boolean.TRUE.equals(needsDisableIndexScan)) {
      LOGGER.info("disableIndexScanIfNeeded:: CQL query requires disabling index scan");
      return conn.execute("SET LOCAL enable_indexscan = OFF;");
    }
    LOGGER.info("disableIndexScanIfNeeded:: CQL query does not require disabling index scan");
    return Future.succeededFuture();
  }

  /**
   * Validates if the CQL query requires disabling index scan.
   *
   * @param cqlQuery The CQL query to validate.
   * @return A Future that completes with true if the query requires disabling index scan, false otherwise.
   */
  public Boolean validateCqlForDisableIndexScan(String cqlQuery) {
    return checkCqlFieldsAndSortConditions(cqlQuery, "items", List.of("description", "userBarcode"), "date");
  }

  /**
   * Checks if the CQL query contains specific fields and sort conditions.
   *
   * @param cqlQuery The CQL query to check.
   * @param fieldName The field name that must exist in the CQL query.
   * @param negativeFieldNames The list of field names that must not exist in the CQL query.
   * @param sortFieldName The sort field name that must exist in the CQL query.
   * @return true if all conditions are met, false otherwise.
   */
  public static Boolean checkCqlFieldsAndSortConditions(String cqlQuery, String fieldName, List<String> negativeFieldNames, String sortFieldName) {
    if (cqlQuery == null) {
      return Boolean.FALSE;
    }

    Boolean isFieldNameExist = Boolean.FALSE;
    Boolean isNegativeFieldsNameExist = Boolean.FALSE;
    Boolean isSortFieldNameExist = Boolean.FALSE;

    List<String> cqlFieldList = new ArrayList<>();
    String cqlSortModifier;
    try {
      CQLParser parser = new CQLParser();
      CQLNode node = parser.parse(cqlQuery);
      node.traverse(new CQLDefaultNodeVisitor() {
        @Override
        public void onTermNode(CQLTermNode node) {
          cqlFieldList.add(node.getIndex());
        }
      });

      cqlSortModifier = node instanceof CQLSortNode cqlSortNode ?
        cqlSortNode.getSortIndexes().stream()
          .findFirst()
          .map(ModifierSet::getBase)
          .orElse("") : "";
    } catch (Exception e) {
      LOGGER.debug("checkConditionExist:: Error parsing CQL cqlQuery: {}", e.getMessage());
      return Boolean.FALSE;
    }

    // ✅ Check if cqlFieldList equals to fieldName
    if (fieldName == null || fieldName.isEmpty() || cqlFieldList.contains(fieldName)) {
      isFieldNameExist = Boolean.TRUE;
    }

    // ✅ Check if cqlSortModifier equals to sortFieldName
    if (sortFieldName == null || sortFieldName.isEmpty() || sortFieldName.equals(cqlSortModifier)) {
      isSortFieldNameExist = Boolean.TRUE;
    }

    // ✅ Check if none of the negativeFieldNames exist in cqlFieldList
    boolean noneNegativeExist = negativeFieldNames == null || negativeFieldNames.isEmpty() ||
                                Collections.disjoint(negativeFieldNames, cqlFieldList);
    if (noneNegativeExist) {
      isNegativeFieldsNameExist = Boolean.TRUE;
    }

    // ✅ Final result: all 3 checks must be true
    return isFieldNameExist && isNegativeFieldsNameExist && isSortFieldNameExist;
  }
}
