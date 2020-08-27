package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.allOf;
import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TenantSampleApi extends TenantAPI {
  private static final Logger log = LoggerFactory.getLogger(TenantSampleApi.class);
  private static final String PARAMETER_LOAD_SAMPLE = "loadSample";
  private static final String SAMPLES_PATH = "samples";

  private static final Map<String, String> sampleDataMap = new HashMap<>();

  static {
    sampleDataMap.put("fees_fines", "fee_fine.json");
    sampleDataMap.put("item_blocks", "item_block.json");
    sampleDataMap.put("loans", "loan.json");
    sampleDataMap.put("manual_blocks", "manual_block.json");
    sampleDataMap.put("notices", "notice.json");
    sampleDataMap.put("patron_blocks", "patron_block.json");
    sampleDataMap.put("requests", "request.json");
  }

  @Override
  public void postTenant(TenantAttributes tenantAttributes, Map<String, String> headers,
    Handler<AsyncResult<Response>> handlers, Context context) {
    super.postTenant(tenantAttributes, headers, res -> {
      if (res.failed() || (res.succeeded() && (res.result().getStatus() < 200 || res.result().getStatus() > 299))) {
        handlers.handle(res);
        return;
      }

      if (isLoadSample(tenantAttributes)) {
        log.info("Loading sample data...");
        String tenantId = TenantTool.calculateTenantId(headers.get(RestVerticle.OKAPI_HEADER_TENANT));

        allOf(sampleDataMap.entrySet().stream()
          .map(e -> loadSample(e.getKey(), getMockAsJson(SAMPLES_PATH + "/" + e.getValue()).mapTo(LogRecord.class), context, tenantId))
          .toArray(CompletableFuture[]::new))
          .thenAccept(vVoid -> handlers.handle(io.vertx.core.Future.succeededFuture(PostTenantResponse
                    .respond201WithApplicationJson(""))))
          .exceptionally(throwable -> {
            handlers.handle(io.vertx.core.Future.succeededFuture(PostTenantResponse
              .respond500WithTextPlain(throwable.getLocalizedMessage())));
            return null;
          });
      } else {
        handlers.handle(res);
      }
    }, context);
  }

  private CompletableFuture<Void> loadSample(String tableName, LogRecord logRecord, Context context, String tenantId) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(context.owner(), tenantId).save(tableName, logRecord, reply -> {
      if (reply.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(reply.cause());
      }
    });
    return future;
  }

  @Override
  public void deleteTenant(Map<String, String> headers, Handler<AsyncResult<Response>> handlers, Context context) {
    super.deleteTenant(headers, res -> {
      Vertx vertx = context.owner();
      String tenantId = TenantTool.tenantId(headers);
      PostgresClient.getInstance(vertx, tenantId)
        .closeClient(event -> handlers.handle(res));
    }, context);
  }

  private boolean isLoadSample(TenantAttributes tenantAttributes) {
    // if a system parameter is passed from command line, ex: loadSample=true
    // that value is considered,Priority of Parameters:
    // Tenant Attributes > command line parameter > default(false)
    boolean loadSample = Boolean.parseBoolean(MODULE_SPECIFIC_ARGS.getOrDefault(PARAMETER_LOAD_SAMPLE,
      "false"));
    List<Parameter> parameters = tenantAttributes.getParameters();
    for (Parameter parameter : parameters) {
      if (PARAMETER_LOAD_SAMPLE.equals(parameter.getKey())) {
        loadSample = Boolean.parseBoolean(parameter.getValue());
      }
    }
    return loadSample;
  }

  private JsonObject getMockAsJson(String fullPath) {
    log.info("Using mock datafile: " + fullPath);
    try (InputStream resourceAsStream = TenantSampleApi.class.getClassLoader().getResourceAsStream(fullPath)) {
      if (resourceAsStream != null) {
        return new JsonObject(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      log.error("Failed to load mock data: " + fullPath, e);
    }
    return new JsonObject();
  }
}
