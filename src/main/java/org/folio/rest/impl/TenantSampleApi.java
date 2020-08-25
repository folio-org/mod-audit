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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TenantSampleApi extends TenantAPI {
  private static final Logger log = LoggerFactory.getLogger(TenantSampleApi.class);
  private static final String PARAMETER_LOAD_SAMPLE = "loadSample";
  private static final String SAMPLES_PATH = "samples";

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

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        listDirContent(SAMPLES_PATH, true)
          .forEach(directory -> listDirContent(SAMPLES_PATH + "/" + directory, false)
            .forEach(fileName -> {
              LogRecord record = getMockAsJson(SAMPLES_PATH + "/" + directory + "/" + fileName).mapTo(LogRecord.class);
              futures.add(loadSample(directory, record, context, tenantId));
            }));

        allOf(futures.toArray(new CompletableFuture[0]))
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
  public void getTenant(Map<String, String> headers, Handler<AsyncResult<Response>> handlers, Context context) {
    super.getTenant(headers, handlers, context);
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

  private Set<String> listDirContent(String path, boolean isDirectory) {
    return Stream.of(new File(TenantSampleApi.class.getClassLoader().getResource(path).getPath()).listFiles())
      .filter(file -> isDirectory == file.isDirectory())
      .map(File::getName)
      .collect(Collectors.toSet());
  }

  private JsonObject getMockAsJson(String fullPath) {
    try {
      return new JsonObject(getMockData(fullPath));
    } catch (IOException e) {
      log.error("Failed to load mock data: " + fullPath, e);
    }
    return new JsonObject();
  }

  private static String getMockData(String path) throws IOException {
    log.info("Using mock datafile: " + path);
    try (InputStream resourceAsStream = TenantSampleApi.class.getClassLoader().getResourceAsStream(path)) {
      if (resourceAsStream != null) {
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      } else {
        StringBuilder sb = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
          lines.forEach(sb::append);
        }
        return sb.toString();
      }
    }
  }
}
