package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.isNull;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.Constants.NO_BARCODE;
import static org.folio.util.ErrorUtils.buildError;

import java.util.*;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import io.vertx.core.Future;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.folio.rest.jaxrs.resource.AuditDataCirculation;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.persist.PgUtil;

public class CirculationLogsService extends BaseService implements AuditDataCirculation {
  public static final String LOGS_TABLE_NAME = "circulation_logs";
  private static final Logger LOGGER = LogManager.getLogger();
  public static final String OPTIMISED_TRUE = "AND optimised=true";

  @Override
  @Validate
  public void getAuditDataCirculationLogs(String query, int offset, int limit, String lang,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {


//    try {
//      this.customQueryBuilder(query,offset,limit,lang,okapiHeaders,asyncResultHandler,vertxContext);
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//      LOGGER.info("exception is:{}",e.getMessage());
//    }

    createCqlWrapper(LOGS_TABLE_NAME, query, limit, offset)
      .thenAccept(cqlWrapper -> {
        LOGGER.info("sql is:{}",cqlWrapper.getWhereClause());
        LOGGER.info("fields: {}",cqlWrapper.getQuery());

        System.out.println("limit-->"+cqlWrapper.getLimit());
        System.out.println("without-->"+cqlWrapper.getWithoutLimOff());

        getClient(okapiHeaders, vertxContext)
          .get(LOGS_TABLE_NAME, LogRecord.class, new String[]{"*"}, cqlWrapper, true, false, reply -> {
            if (reply.succeeded()) {
              var results = reply.result().getResults();
              results.stream().filter(logRecord -> isNull(logRecord.getUserBarcode()))
                .forEach(logRecord -> logRecord.setUserBarcode(NO_BARCODE));
              // added to test only for item query considering to sort param passed
              if (query!=null && query.contains(OPTIMISED_TRUE)) {
                System.out.println("inside if--");
                results.sort((res1,res2)-> res2.getDate().compareTo(res1.getDate()));

                results = results.stream().skip(offset).limit(limit).collect(Collectors.toList());
              }
              // end
              asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
                .respond200WithApplicationJson(new LogRecordCollection()
                  .withLogRecords(results)
                  .withTotalRecords(reply.result().getResultInfo().getTotalRecords()))));
            } else {
              asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
                .respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), reply.cause().getLocalizedMessage()))));
            }
          });
      })
      .exceptionally(throwable -> {
        asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse.
          respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), throwable.getLocalizedMessage()))));
        return null;
      });
  }


  public void customQueryBuilder(String q, int offset, int limit, String lang,
                   Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {


    getClient(okapiHeaders, vertxContext).getReadConnection((res) -> {

      PgConnection connection = res.result();

      String sql = "select id, jsonb from dikuvolaris_mod_audit.circulation_logs where dikuvolaris_mod_audit.get_tsvector(dikuvolaris_mod_audit.f_unaccent(jsonb ->> 'source'::text))@@ plainto_tsquery('%Svoom%')";


      connection
        .query(sql)
        .execute()
        .onComplete(ar -> {
          if (ar.succeeded()) {
            RowSet<Row> result = ar.result();
            System.out.println("Got " + result.size() + " rows ");

            Iterator<Row> iterator = result.iterator();
            while(iterator.hasNext()) {
              Row row = iterator.next();
              LOGGER.info("data is:{}",row.toJson());

              LogRecord logRecord = row.toJson().getJsonObject("jsonb").mapTo(LogRecord.class);

              LOGGER.info("record is:{}",logRecord);
              LOGGER.info("record is:{}",logRecord.getSource());

              // result.put(row.getValue(0).toString(), function.apply(row.getValue(1).toString()));
            }

          } else {
            System.out.println("Failure: " + ar.cause().getMessage());
          }

          // Now close the pool
          connection.close();
        });


      });

    /*
    getClient(okapiHeaders, vertxContext).getReadConnection((res) -> {

        Tuple list = Tuple.tuple();

        PgConnection connection = res.result();
        String sql = "select id, jsonb from dikuvolaris_mod_audit.circulation_logs where dikuvolaris_mod_audit.get_tsvector(dikuvolaris_mod_audit.f_unaccent(jsonb ->> 'source'::text))@@ plainto_tsquery('%Svoom%')";


        connection.preparedQuery(sql.toString()).execute(list, (query) -> {
          connection.close();
          if (query.failed()) {
            replyHandler.handle(Future.failedFuture(query.cause()));
          } else {
            try {
              Map<String, ?> result = new HashMap();
              Iterator<Row> iterator = ((RowSet)query.result()).iterator();

              while(iterator.hasNext()) {
                Row row = (Row)iterator.next();
                System.out.println("data is->"+row.getValue(0).toString());
               // result.put(row.getValue(0).toString(), function.apply(row.getValue(1).toString()));
              }

              replyHandler.handle(Future.succeededFuture(result));
            } catch (Exception e) {
              replyHandler.handle(Future.failedFuture(e));
            }

          }
        });
    });

     */
  }
}
