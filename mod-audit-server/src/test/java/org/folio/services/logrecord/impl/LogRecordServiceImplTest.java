package org.folio.services.logrecord.impl;

import static org.folio.rest.impl.CirculationLogsService.LOGS_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.builder.LogRecordBuilderResolver;
import org.folio.builder.service.LogRecordBuilder;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class LogRecordServiceImplTest {
  private static final String TENANT_ID = "diku";
  private static final String LOG_EVENT_TYPE = "CHECK_IN_EVENT";

  @Mock
  private PostgresClientFactory pgClientFactory;
  @Mock
  private PostgresClient postgresClient;

  private LogRecordServiceImpl logRecordService;
  private Map<String, String> okapiHeaders;

  @BeforeEach
  void setUp() {
    okapiHeaders = Map.of(RestVerticle.OKAPI_HEADER_TENANT, TENANT_ID);
    logRecordService = new LogRecordServiceImpl(pgClientFactory);
    doReturn(postgresClient).when(pgClientFactory).createInstance(TENANT_ID);
  }

  @Test
  void shouldSaveLogRecordsWhenNoAnonymizeActionPresent(VertxTestContext ctx) {
    var payload = new JsonObject();
    var logRecord = new LogRecord().withAction(LogRecord.Action.CHECKED_IN);
    var builder = mockBuilderReturning(payload, List.of(logRecord));

    when(postgresClient.upsertBatch(eq(LOGS_TABLE_NAME), eq(List.of(logRecord))))
      .thenReturn(Future.succeededFuture(mock(RowSet.class)));

    try (var resolver = mockStatic(LogRecordBuilderResolver.class)) {
      resolver.when(() -> LogRecordBuilderResolver.getBuilder(eq(LOG_EVENT_TYPE), eq(okapiHeaders), eq(null)))
        .thenReturn(builder);

      var future = logRecordService.processLogRecord(LOG_EVENT_TYPE, payload, okapiHeaders, null);

      future.onComplete(ctx.succeeding(result -> {
        verify(postgresClient, times(1)).upsertBatch(eq(LOGS_TABLE_NAME), eq(List.of(logRecord)));
        verify(postgresClient, never()).get(any(), any(), any(String[].class), any(), anyBoolean(), anyBoolean(), any(Handler.class));
        ctx.completeNow();
      }));
    }
  }

  @Test
  void shouldAnonymizeLoanRelatedRecordsBeforeSavingWhenAnonymizeActionPresent(VertxTestContext ctx) {
    var payload = new JsonObject();
    var loanId = UUID.randomUUID().toString();
    var firstItem = new Item().withLoanId(loanId);
    var anonymizeRecord = new LogRecord().withAction(LogRecord.Action.ANONYMIZE).withItems(List.of(firstItem));
    var builder = mockBuilderReturning(payload, List.of(anonymizeRecord));

    var relatedUserId = UUID.randomUUID().toString();
    var relatedRecord = new LogRecord()
      .withUserBarcode("user-barcode")
      .withLinkToIds(new LinkToIds().withUserId(relatedUserId));

    var results = new Results<LogRecord>();
    results.setResults(List.of(relatedRecord));

    doAnswer(invocation -> {
      Handler<AsyncResult<Results<LogRecord>>> replyHandler = invocation.getArgument(6);
      replyHandler.handle(Future.succeededFuture(results));
      return null;
    }).when(postgresClient).get(eq(LOGS_TABLE_NAME), eq(LogRecord.class), any(String[].class), any(), eq(false), eq(false), any(Handler.class));

    when(postgresClient.upsertBatch(eq(LOGS_TABLE_NAME), eq(List.of(anonymizeRecord, relatedRecord))))
      .thenReturn(Future.succeededFuture(mock(RowSet.class)));

    try (var resolver = mockStatic(LogRecordBuilderResolver.class)) {
      resolver.when(() -> LogRecordBuilderResolver.getBuilder(eq(LOG_EVENT_TYPE), eq(okapiHeaders), eq(null)))
        .thenReturn(builder);

      var future = logRecordService.processLogRecord(LOG_EVENT_TYPE, payload, okapiHeaders, null);

      future.onComplete(ctx.succeeding(result -> {
        assertTrue(relatedRecord.getUserBarcode() == null);
        assertTrue(relatedRecord.getLinkToIds().getUserId() == null);
        verify(postgresClient, times(1)).get(eq(LOGS_TABLE_NAME), eq(LogRecord.class), any(String[].class), any(), eq(false), eq(false), any(Handler.class));
        verify(postgresClient, times(1)).upsertBatch(eq(LOGS_TABLE_NAME), eq(List.of(anonymizeRecord, relatedRecord)));
        ctx.completeNow();
      }));
    }
  }

  @Test
  void shouldFailWhenSaveLogRecordsFails(VertxTestContext ctx) {
    var payload = new JsonObject();
    var logRecord = new LogRecord().withAction(LogRecord.Action.CHECKED_IN);
    var builder = mockBuilderReturning(payload, List.of(logRecord));

    when(postgresClient.upsertBatch(eq(LOGS_TABLE_NAME), eq(List.of(logRecord))))
      .thenReturn(Future.failedFuture(new RuntimeException("db error")));

    try (var resolver = mockStatic(LogRecordBuilderResolver.class)) {
      resolver.when(() -> LogRecordBuilderResolver.getBuilder(eq(LOG_EVENT_TYPE), eq(okapiHeaders), eq(null)))
        .thenReturn(builder);

      var future = logRecordService.processLogRecord(LOG_EVENT_TYPE, payload, okapiHeaders, null);

      future.onComplete(ctx.failing(cause -> ctx.completeNow()));
    }
  }

  private LogRecordBuilder mockBuilderReturning(JsonObject payload, List<LogRecord> logRecords) {
    var builder = mock(LogRecordBuilder.class);
    when(builder.buildLogRecord(eq(payload))).thenReturn(CompletableFuture.completedFuture(logRecords));
    return builder;
  }
}
