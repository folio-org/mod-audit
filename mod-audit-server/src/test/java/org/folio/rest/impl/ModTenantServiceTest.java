package org.folio.rest.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.errors.TopicExistsException;
import org.folio.kafka.services.KafkaAdminClientService;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.management.AuditManager;
import org.folio.util.pubsub.PubSubClientUtils;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
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
  private Vertx vertx;

  private ModTenantService service;

  @BeforeEach
  void setUp() {
    service = new ModTenantService(auditManager);
  }

  @Test
  void createTopicsIfEnabledCreatesTopicsWhenModuleEnabled() {
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.succeededFuture()))) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertTrue(result.succeeded());
      verify(mocked.constructed().get(0)).createKafkaTopics(any(), any());
    }
  }

  @Test
  void createTopicsIfEnabledDoesNothingWhenModuleToIsNull() {
    var attributes = new TenantAttributes();
    try (var mocked = mockConstruction(KafkaAdminClientService.class)) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertTrue(result.succeeded());
      assertTrue(mocked.constructed().isEmpty());
    }
  }

  @Test
  void createTopicsIfEnabledSucceedsWhenTopicsAlreadyExist() {
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    var topicExistsException = new TopicExistsException("already exists");
    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.failedFuture(topicExistsException)))) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertTrue(result.succeeded());
    }
  }

  @Test
  void createTopicsIfEnabledFailsOnUnexpectedError() {
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.failedFuture(new RuntimeException("boom"))))) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertTrue(result.failed());
    }
  }

  @Test
  void deleteTopicsIfPurgingDeletesTopicsWhenPurgeTrue() {
    var attributes = new TenantAttributes().withPurge(Boolean.TRUE);
    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.deleteKafkaTopics(any(), any())).thenReturn(Future.succeededFuture()))) {
      Future<Void> result = service.deleteTopicsIfPurging(attributes, vertx, TENANT_ID);
      assertTrue(result.succeeded());
      verify(mocked.constructed().get(0)).deleteKafkaTopics(any(), any());
    }
  }

  @Test
  void deleteTopicsIfPurgingDoesNothingWhenPurgeFalse() {
    var attributes = new TenantAttributes();
    try (var mocked = mockConstruction(KafkaAdminClientService.class)) {
      Future<Void> result = service.deleteTopicsIfPurging(attributes, vertx, TENANT_ID);
      assertTrue(result.succeeded());
      assertTrue(mocked.constructed().isEmpty());
    }
  }

  @Test
  void deleteTopicsIfPurgingDoesNothingWhenModuleToIsNotNull() {
    var attributes = new TenantAttributes().withModuleTo("1.0.0").withPurge(Boolean.TRUE);
    try (var mocked = mockConstruction(KafkaAdminClientService.class)) {
      Future<Void> result = service.deleteTopicsIfPurging(attributes, vertx, TENANT_ID);
      assertTrue(result.succeeded());
      assertTrue(mocked.constructed().isEmpty());
    }
  }

  @Test
  void loadDataRegistersToPubSubAndRunsDatabaseCleanup() throws InterruptedException {
    var attributes = new TenantAttributes(); // moduleTo == null: no topic creation
    var context = mock(Context.class);
    when(context.owner()).thenReturn(vertx);
    when(auditManager.executeDatabaseCleanup(TENANT_ID)).thenReturn(Future.succeededFuture());

    var latch = new CountDownLatch(1);
    try (var pubSubMock = mockStatic(PubSubClientUtils.class)) {
      pubSubMock.when(() -> PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
        .thenReturn(CompletableFuture.completedFuture(true));

      service.loadData(attributes, TENANT_ID, Map.of(), context)
        .onComplete(ar -> latch.countDown());

      assertTrue(latch.await(2, TimeUnit.SECONDS));
      verify(auditManager).executeDatabaseCleanup(TENANT_ID);
    }
  }
}
