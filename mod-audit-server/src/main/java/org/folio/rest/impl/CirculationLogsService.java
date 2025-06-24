package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.isNull;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.Constants.NO_BARCODE;
import static org.folio.util.ErrorUtils.buildError;

import java.io.IOException;
import java.util.ArrayList;
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
import org.folio.rest.persist.interfaces.Results;
import org.z3950.zing.cql.CQLDefaultNodeVisitor;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.ModifierSet;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

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
        .withReadTrans(conn -> validateCqlForDisableIndexScan(query)
          .compose(needsDisableIndexScan -> {
            if (Boolean.TRUE.equals(needsDisableIndexScan)) {
              LOGGER.info("getAuditDataCirculationLogs:: CQL query requires disabling index scan");
              return conn.execute("SET LOCAL enable_indexscan = OFF;")
                .compose(rows -> conn.get(LOGS_TABLE_NAME, LogRecord.class, cqlWrapper, true));
            }
            LOGGER.info("getAuditDataCirculationLogs:: CQL query does not require disabling index scan");
            return conn.get(LOGS_TABLE_NAME, LogRecord.class, cqlWrapper, true);
          }))
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
   * Validates if the CQL query requires disabling index scan.
   *
   * @param cqlQuery The CQL query to validate.
   * @return A Future that completes with true if the query requires disabling index scan, false otherwise.
   */
  public Future<Boolean> validateCqlForDisableIndexScan(String cqlQuery) {
    return Future.succeededFuture(checkCqlFieldsAndSortConditions(cqlQuery, List.of("items"), List.of("description", "userBarcode"), List.of("date")));
  }

  /**
   * Checks if the CQL query contains specific fields and sort conditions.
   *
   * @param cqlQuery The CQL query to check.
   * @param fieldNames The list of field names that must exist in the CQL query.
   * @param negativeFieldNames The list of field names that must not exist in the CQL query.
   * @param sortFieldsNames The list of sort field names that must exist in the CQL query.
   * @return true if all conditions are met, false otherwise.
   */
  public static Boolean checkCqlFieldsAndSortConditions(String cqlQuery, List<String> fieldNames, List<String> negativeFieldNames, List<String> sortFieldsNames) {
    Boolean isFieldsNameExist = Boolean.FALSE;
    Boolean isNegativeFieldsNameExist = Boolean.FALSE;
    Boolean isSortFieldsNameExist = Boolean.FALSE;

    List<String> cqlFieldList = new ArrayList<>();
    List<String> cqlSortModifiers = new ArrayList<>();
    try {
      CQLParser parser = new CQLParser();
      CQLNode node = parser.parse(cqlQuery);
      node.traverse(new CQLDefaultNodeVisitor() {
        @Override
        public void onTermNode(CQLTermNode node) {
          cqlFieldList.add(node.getIndex());
        }
      });

      if(node instanceof CQLSortNode cqlSortNode) {
        cqlSortModifiers.addAll(cqlSortNode.getSortIndexes().stream().map(ModifierSet::getBase).toList());
      }
    } catch (CQLParseException | IOException e) {
      LOGGER.debug("checkConditionExist:: Error parsing CQL cqlQuery: {}", e.getMessage());
    }

    // ✅ Check if all fieldNames exist in cqlFieldList
    if (fieldNames == null || fieldNames.isEmpty() || cqlFieldList.containsAll(fieldNames)) {
      isFieldsNameExist = Boolean.TRUE;
    }

    // ✅ Check if all sortFieldsNames exist in cqlSortModifiers
   if (sortFieldsNames == null || sortFieldsNames.isEmpty() || cqlSortModifiers.containsAll(sortFieldsNames)) {
      isSortFieldsNameExist = Boolean.TRUE;
    }

    // ✅ Check if none of the negativeFieldNames exist in cqlFieldList
    boolean noneNegativeExist = negativeFieldNames == null || negativeFieldNames.isEmpty() ||
                                negativeFieldNames.stream().noneMatch(cqlFieldList::contains);
    if (noneNegativeExist) {
      isNegativeFieldsNameExist = Boolean.TRUE;
    }

    // ✅ Final result: all 3 checks must be true
    return isFieldsNameExist && isNegativeFieldsNameExist && isSortFieldsNameExist;
  }
}
