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

  private static final CQLParser CQL_PARSER = new CQLParser();

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
    boolean needsDisableIndexScan = validateCqlForDisableIndexScan(query);
    if (needsDisableIndexScan) {
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
   * @return boolean true if the query requires disabling index scan, false otherwise.
   */
  public static boolean validateCqlForDisableIndexScan(String cqlQuery) {
    if (cqlQuery == null) {
      return false;
    }

    List<String> cqlFieldList = new ArrayList<>();
    String cqlSortModifier;
    try {
      CQLNode node = CQL_PARSER.parse(cqlQuery);
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
      return false;
    }

    return cqlFieldList.contains("items")
           && Collections.disjoint(cqlFieldList, List.of("description", "userBarcode"))
           && "date".equals(cqlSortModifier);
  }
}
