package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.util.OkapiConnectionParams;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.folio.services.management.AuditManager;
import org.folio.spring.SpringContextUtil;
import org.folio.util.pubsub.PubSubClientUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class ModTenantService extends TenantAPI {
  private static final Logger log = LogManager.getLogger();

  @Autowired
  private AuditManager auditManager;

  public ModTenantService() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public Future<Integer> loadData(TenantAttributes attributes, String tenantId, Map<String, String> headers, Context context) {
    log.debug("loadData:: Starting loadData");
    Promise<Integer> promise = Promise.promise();
    registerModuleToPubSub(headers, context.owner())
      .thenAccept(p -> promise.complete(0));
    log.info("loadData:: Started Loading Data");
    return promise.future()
      .compose(integer -> auditManager.executeDatabaseCleanup(tenantId).map(integer));
  }

  private CompletableFuture<Void> registerModuleToPubSub(Map<String, String> headers, Vertx vertx) {
    log.debug("registerModuleToPubSub:: Registering ModuleToPubSub");
    CompletableFuture<Void> future = new CompletableFuture<>();
    CompletableFuture.supplyAsync(() -> PubSubClientUtils.registerModule(new OkapiConnectionParams(headers, vertx)))
      .thenAccept(registered -> {
        log.info("registerModuleToPubSub:: Module registered successfully");
        future.complete(null);
      })
      .exceptionally(throwable -> {
        log.warn("Error occurred while registering module: {}", throwable.getMessage());
        future.completeExceptionally(throwable);
        return null;
      });
    log.info("registerModuleToPubSub:: Registered ModuleToPubSub Successfully");
    return future;
  }
}
