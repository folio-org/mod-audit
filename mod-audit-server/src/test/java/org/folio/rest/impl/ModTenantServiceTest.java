package org.folio.rest.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.kafka.common.errors.TopicExistsException;
import org.folio.kafka.services.KafkaAdminClientService;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.management.AuditManager;
import org.folio.util.AuditKafkaTopic;
import org.folio.util.pubsub.PubSubClientUtils;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModTenantServiceTest {

  private static final String TENANT_ID = "test-tenant";

  @Mock
  private AuditManager auditManager;
  @Mock
  private Context context;
  @Mock
  private Vertx vertx;

  @Test
  void kafkaTopicsActionCreatesTopicsOnTenantEnable() {
    try (var construction = mockConstruction(KafkaAdminClientService.class,
        (mock, ctx) -> when(mock.createKafkaTopics(any(), anyString()))
          .thenReturn(Future.succeededFuture()))) {

      new ModTenantService(auditManager).kafkaTopicsAction(new TenantAttributes(), vertx, TENANT_ID);

      assertThat(construction.constructed()).hasSize(1);
      verify(construction.constructed().get(0)).createKafkaTopics(AuditKafkaTopic.values(), TENANT_ID);
    }
  }

  @Test
  void kafkaTopicsActionDeletesTopicsOnTenantDisable() {
    var attributes = new TenantAttributes().withPurge(Boolean.TRUE);

    try (var construction = mockConstruction(KafkaAdminClientService.class,
        (mock, ctx) -> when(mock.deleteKafkaTopics(any(), anyString()))
          .thenReturn(Future.succeededFuture()))) {

      new ModTenantService(auditManager).kafkaTopicsAction(attributes, vertx, TENANT_ID);

      assertThat(construction.constructed()).hasSize(1);
      verify(construction.constructed().get(0)).deleteKafkaTopics(AuditKafkaTopic.values(), TENANT_ID);
    }
  }

  @Test
  void kafkaTopicsActionConstructsKafkaAdminClientServiceWithVertxFromContext() {
    List<List<?>> capturedConstructorArgs = new ArrayList<>();

    try (var construction = mockConstruction(KafkaAdminClientService.class,
        (mock, ctx) -> {
          capturedConstructorArgs.add(ctx.arguments());
          when(mock.createKafkaTopics(any(), anyString())).thenReturn(Future.succeededFuture());
        })) {

      new ModTenantService(auditManager).kafkaTopicsAction(new TenantAttributes(), vertx, TENANT_ID);

      assertThat(capturedConstructorArgs).hasSize(1);
      assertThat(capturedConstructorArgs.get(0)).hasSize(1);
      assertThat(capturedConstructorArgs.get(0).get(0)).isEqualTo(vertx);
    }
  }

  @Test
  void kafkaTopicsActionSucceedsWhenTopicsAlreadyExist() {
    try (var ignored = mockConstruction(KafkaAdminClientService.class,
        (mock, ctx) -> when(mock.createKafkaTopics(any(), anyString()))
          .thenReturn(Future.failedFuture(new TopicExistsException("topic already exists"))))) {

      var result = new ModTenantService(auditManager)
        .kafkaTopicsAction(new TenantAttributes(), vertx, TENANT_ID);

      assertThat(result.succeeded()).isTrue();
    }
  }

  @Test
  void kafkaTopicsActionFailsWhenTopicCreationFailsWithUnexpectedError() {
    var error = new RuntimeException("kafka unavailable");

    try (var ignored = mockConstruction(KafkaAdminClientService.class,
        (mock, ctx) -> when(mock.createKafkaTopics(any(), anyString()))
          .thenReturn(Future.failedFuture(error)))) {

      var result = new ModTenantService(auditManager)
        .kafkaTopicsAction(new TenantAttributes(), vertx, TENANT_ID);

      assertThat(result.failed()).isTrue();
      assertThat(result.cause()).isEqualTo(error);
    }
  }

  @Test
  void loadDataRegistersToPubSubAndRunsDatabaseCleanup() throws InterruptedException {
    when(context.owner()).thenReturn(vertx);
    when(auditManager.executeDatabaseCleanup(TENANT_ID)).thenReturn(Future.succeededFuture());

    var latch = new CountDownLatch(1);

    try (var pubSubMock = mockStatic(PubSubClientUtils.class)) {
      pubSubMock.when(() -> PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
        .thenReturn(CompletableFuture.completedFuture(true));

      new ModTenantService(auditManager)
        .loadData(new TenantAttributes(), TENANT_ID, Map.of(), context)
        .onComplete(ar -> latch.countDown());

      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
      verify(auditManager).executeDatabaseCleanup(TENANT_ID);
    }
  }
}
