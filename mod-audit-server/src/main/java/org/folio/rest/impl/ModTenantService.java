package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.allOf;
import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;
import static org.folio.rest.impl.CirculationLogsService.LOGS_TABLE_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ModTenantService extends TenantAPI {
  private static final Logger log = LogManager.getLogger();
  private static final String PARAMETER_LOAD_SAMPLE = "loadSample";
  private static final String SAMPLES_PATH = "samples";

  private final List<String> samples = Arrays.asList("fee_fine.json", "item_block.json", "loan.json", "manual_block.json",
    "notice.json", "patron_block.json", "request.json");

  @Override
  public Future<Integer> loadData(TenantAttributes attributes, String tenantId, Map<String, String> headers, Context context) {
    log.info("postTenant");
    Vertx vertx = context.owner();
    Promise<Integer> promise = Promise.promise();
    TenantLoading tl = new TenantLoading();
    tl.perform(attributes, headers, vertx, res1 -> {
      if (res1.failed()) {
        promise.fail(res1.cause());
      } else {
        registerModuleToPubSub(headers, vertx)
          .thenCompose(vVoid -> loadSampleData(attributes, headers, context)).thenAccept(n -> {
          System.out.println();
          promise.complete(n);
        });
      }
    });

    return promise.future();
  }

  private CompletableFuture<Integer> loadSampleData(TenantAttributes tenantAttributes, Map<String, String> headers,
    Context context) {
    CompletableFuture<Integer> future = new CompletableFuture<>();
    if (isLoadSample(tenantAttributes)) {
      log.info("Loading sample data...");

      String tenantId = TenantTool.calculateTenantId(headers.get(RestVerticle.OKAPI_HEADER_TENANT));

      allOf(samples.stream()
        .map(fileName -> loadSample(LOGS_TABLE_NAME, fileName, context, tenantId))
        .toArray(CompletableFuture[]::new))
        .thenAccept(vVoid -> {
          log.info("Sample data loaded successfully");
          future.complete(samples.size());
        })
        .exceptionally(throwable -> {
          future.completeExceptionally(throwable);
          return null;
        });
    } else {
      future.complete(0);
    }
    return future;
  }

  private CompletableFuture<Void> loadSample(String tableName, String sampleFileName, Context context, String tenantId) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    try {
      PostgresClient.getInstance(context.owner(), tenantId)
        .save(tableName, getSampleAsJson(SAMPLES_PATH + "/" + sampleFileName).mapTo(LogRecord.class), reply -> {
          if (reply.succeeded()) {
            future.complete(null);
          } else {
            future.completeExceptionally(reply.cause());
          }
        });
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  @Override
  public void deleteTenantByOperationId(String operationId, Map<String, String> headers, Handler<AsyncResult<Response>> hndlr,
                                        Context cntxt) {
    log.info("deleteTenant");
    super.deleteTenantByOperationId(operationId, headers, res -> {
      String tenantId = TenantTool.tenantId(headers);
      PostgresClient.getInstance(cntxt.owner(), tenantId)
        .closeClient(event -> hndlr.handle(res));
    }, cntxt);
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

  private JsonObject getSampleAsJson(String fullPath) throws IOException {
    log.info("Using mock datafile: " + fullPath);
    try (InputStream resourceAsStream = ModTenantService.class.getClassLoader().getResourceAsStream(fullPath)) {
      if (resourceAsStream != null) {
        return new JsonObject(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
      }
    }
    throw new IOException("Error loading sample file");
  }

  private CompletableFuture<Void> registerModuleToPubSub(Map<String, String> headers, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    CompletableFuture.supplyAsync(() -> PubSubClientUtils.registerModule(new OkapiConnectionParams(headers, vertx)))
      .thenAccept(registered -> future.complete(null))
      .exceptionally(throwable -> {
        future.completeExceptionally(throwable);
        return null;
      });
    return future;
  }
}
