package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.services.KafkaAdminClientService;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.management.AuditManager;
import org.folio.spring.SpringContextUtil;
import org.folio.util.AuditKafkaTopic;
import org.folio.util.pubsub.PubSubClientUtils;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ModTenantService extends TenantAPI {
  private static final Logger log = LogManager.getLogger();

  @Autowired
  private AuditManager auditManager;

  public ModTenantService() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  // package-private: used by unit tests to inject dependencies without a Spring context
  ModTenantService(AuditManager auditManager) {
    this.auditManager = auditManager;
  }

  /**
   * Phase 1: PubSub and Kafka run in parallel.
   * <ul>
   *   <li>tenant enable → register with PubSub + create Kafka topics</li>
   *   <li>tenant disable ({@code purge=true}) → unregister from PubSub + delete Kafka topics</li>
   * </ul>
   */
  @Override
  public Future<Integer> loadData(TenantAttributes attributes, String tenantId, Map<String, String> headers, Context context) {
    log.debug("loadData:: Starting loadData");
    Vertx vertx = context.owner();

    if (Boolean.TRUE.equals(attributes.getPurge())) {
      unregisterModuleFromPubSub(headers, vertx);
      return deleteKafkaTopics(vertx, tenantId)
        .map(v -> 0)
        .compose(integer -> auditManager.executeDatabaseCleanup(tenantId).map(integer));
    }

    registerModuleToPubSub(headers, vertx);
    log.info("loadData:: Started Loading Data");
    return createKafkaTopics(vertx, tenantId)
      .map(v -> 0)
      .compose(integer -> auditManager.executeDatabaseCleanup(tenantId).map(integer));
  }

  private Future<Void> createKafkaTopics(Vertx vertx, String tenantId) {
    log.debug("createKafkaTopics:: Creating Kafka topics for tenant {}", tenantId);
    return new KafkaAdminClientService(vertx).createKafkaTopics(AuditKafkaTopic.values(), tenantId)
      .onSuccess(v -> log.info("createKafkaTopics:: Kafka topics created successfully for tenant {}", tenantId))
      .onFailure(t -> log.warn("createKafkaTopics:: Failed to create Kafka topics for tenant {}: {}", tenantId, t.getMessage()));
  }

  private Future<Void> deleteKafkaTopics(Vertx vertx, String tenantId) {
    log.debug("deleteKafkaTopics:: Deleting Kafka topics for tenant {}", tenantId);
    return new KafkaAdminClientService(vertx).deleteKafkaTopics(AuditKafkaTopic.values(), tenantId)
      .onSuccess(v -> log.info("deleteKafkaTopics:: Kafka topics deleted successfully for tenant {}", tenantId))
      .onFailure(t -> log.warn("deleteKafkaTopics:: Failed to delete Kafka topics for tenant {}: {}", tenantId, t.getMessage()));
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
    return future;
  }

  private CompletableFuture<Void> unregisterModuleFromPubSub(Map<String, String> headers, Vertx vertx) {
    log.debug("unregisterModuleFromPubSub:: Unregistering module from PubSub");
    CompletableFuture<Void> future = new CompletableFuture<>();
    CompletableFuture.supplyAsync(() -> PubSubClientUtils.unregisterModule(new OkapiConnectionParams(headers, vertx)))
      .thenAccept(unregistered -> {
        log.info("unregisterModuleFromPubSub:: Module unregistered successfully");
        future.complete(null);
      })
      .exceptionally(throwable -> {
        log.warn("unregisterModuleFromPubSub:: Error unregistering module: {}", throwable.getMessage());
        future.completeExceptionally(throwable);
        return null;
      });
    return future;
  }
}
