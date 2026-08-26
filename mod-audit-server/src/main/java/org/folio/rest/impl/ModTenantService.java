package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.Response;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.services.KafkaAdminClientService;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.management.AuditManager;
import org.folio.spring.SpringContextUtil;
import org.folio.util.AuditKafkaTopic;
import org.folio.util.pubsub.PubSubClientUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class ModTenantService extends TenantAPI {

  private static final Logger log = LogManager.getLogger();

  @Autowired
  private AuditManager auditManager;

  public ModTenantService() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  ModTenantService(AuditManager auditManager) {
    this.auditManager = auditManager;
  }

  @Override
  public void postTenant(TenantAttributes attributes, Map<String, String> headers,
    Handler<AsyncResult<Response>> handler, Context context) {

    String tenantId = TenantTool.tenantId(headers);
    Vertx vertx = context.owner();
    deleteTopicsIfPurging(attributes, vertx, tenantId)
      .onComplete(ar -> {
        if (ar.failed()) {
          log.warn("postTenant:: Kafka topics deletion failed for tenant {}", tenantId, ar.cause());
        }
        super.postTenant(attributes, headers, handler, context);
      });
  }

  @Override
  public Future<Integer> loadData(TenantAttributes attributes, String tenantId,
    Map<String, String> headers, Context context) {

    log.debug("loadData:: Starting loadData");
    Vertx vertx = context.owner();
    Promise<Integer> promise = Promise.promise();
    createTopicsIfEnabled(attributes, vertx, tenantId)
      .onComplete(kafkaAr -> {
        if (kafkaAr.failed()) {
          log.warn("loadData:: Kafka topics creation failed for tenant {}", tenantId, kafkaAr.cause());
        }
        registerModuleToPubSub(headers, vertx)
          .thenAccept(v -> promise.complete(0))
          .exceptionally(t -> {
            log.warn("loadData:: PubSub registration failed for tenant {}", tenantId, t);
            promise.fail(t);
            return null;
          });
      });
    log.info("loadData:: Started Loading Data");
    return promise.future()
      .compose(integer -> auditManager.executeDatabaseCleanup(tenantId).map(integer));
  }

  Future<Void> createTopicsIfEnabled(TenantAttributes attributes, Vertx vertx, String tenantId) {
    if (attributes.getModuleTo() != null) {
      return createKafkaTopics(vertx, tenantId);
    }
    return succeededFuture();
  }

  Future<Void> deleteTopicsIfPurging(TenantAttributes attributes, Vertx vertx, String tenantId) {
    if (attributes.getModuleTo() == null && Boolean.TRUE.equals(attributes.getPurge())) {
      return deleteKafkaTopics(vertx, tenantId);
    }
    return succeededFuture();
  }

  private Future<Void> createKafkaTopics(Vertx vertx, String tenantId) {
    log.debug("createKafkaTopics:: Creating Kafka topics for tenant {}", tenantId);
    return new KafkaAdminClientService(vertx).createKafkaTopics(AuditKafkaTopic.values(), tenantId)
      .onSuccess(v -> log.info("createKafkaTopics:: Kafka topics created successfully for tenant {}", tenantId))
      .recover(t -> {
        if (t instanceof TopicExistsException || t.getCause() instanceof TopicExistsException) {
          log.info("createKafkaTopics:: Topics already exist for tenant {}, skipping", tenantId);
          return succeededFuture();
        }
        log.warn("createKafkaTopics:: Failed to create Kafka topics for tenant {}", tenantId, t);
        return failedFuture(t);
      });
  }

  private Future<Void> deleteKafkaTopics(Vertx vertx, String tenantId) {
    log.debug("deleteKafkaTopics:: Deleting Kafka topics for tenant {}", tenantId);
    return new KafkaAdminClientService(vertx).deleteKafkaTopics(AuditKafkaTopic.values(), tenantId)
      .onSuccess(v -> log.info("deleteKafkaTopics:: Kafka topics deleted successfully for tenant {}", tenantId))
      .onFailure(t -> log.warn("deleteKafkaTopics:: Failed to delete Kafka topics for tenant {}", tenantId, t));
  }

  private CompletableFuture<Void> registerModuleToPubSub(Map<String, String> headers, Vertx vertx) {
    log.debug("registerModuleToPubSub:: Registering module to PubSub");
    return CompletableFuture
      .supplyAsync(() -> PubSubClientUtils.registerModule(new OkapiConnectionParams(headers, vertx)))
      .thenAccept(registered -> log.info("registerModuleToPubSub:: Module registered successfully"));
  }
}
