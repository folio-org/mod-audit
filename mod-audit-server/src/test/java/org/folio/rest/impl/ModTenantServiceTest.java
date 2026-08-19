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

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@UnitTest
@ExtendWith(MockitoExtension.class)
// Lenient because stubs for auditManager and PubSubClientUtils live in async compose
// steps that may not complete within the synchronous test execution.
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
  void loadDataCreatesKafkaTopicsOnTenantEnable() {
    when(context.owner()).thenReturn(vertx);
    when(auditManager.executeDatabaseCleanup(TENANT_ID)).thenReturn(Future.succeededFuture());

    try (var pubSubMock = mockStatic(PubSubClientUtils.class);
         var construction = mockConstruction(KafkaAdminClientService.class,
           (mock, ctx) -> when(mock.createKafkaTopics(any(), anyString()))
             .thenReturn(Future.succeededFuture()))) {

      pubSubMock.when(() -> PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
        .thenReturn(CompletableFuture.completedFuture(true));

      new ModTenantService(auditManager).loadData(new TenantAttributes(), TENANT_ID, Map.of(), context);

      assertThat(construction.constructed()).hasSize(1);
      verify(construction.constructed().get(0)).createKafkaTopics(AuditKafkaTopic.values(), TENANT_ID);
    }
  }

  @Test
  void loadDataDeletesKafkaTopicsOnTenantDisable() {
    when(context.owner()).thenReturn(vertx);
    when(auditManager.executeDatabaseCleanup(TENANT_ID)).thenReturn(Future.succeededFuture());

    try (var pubSubMock = mockStatic(PubSubClientUtils.class);
         var construction = mockConstruction(KafkaAdminClientService.class,
           (mock, ctx) -> when(mock.deleteKafkaTopics(any(), anyString()))
             .thenReturn(Future.succeededFuture()))) {

      pubSubMock.when(() -> PubSubClientUtils.unregisterModule(any(OkapiConnectionParams.class)))
        .thenReturn(CompletableFuture.completedFuture(true));

      var attributes = new TenantAttributes().withPurge(Boolean.TRUE);
      new ModTenantService(auditManager).loadData(attributes, TENANT_ID, Map.of(), context);

      assertThat(construction.constructed()).hasSize(1);
      verify(construction.constructed().get(0)).deleteKafkaTopics(AuditKafkaTopic.values(), TENANT_ID);
    }
  }

  @Test
  void loadDataConstructsKafkaAdminClientServiceWithVertxFromContext() {
    when(context.owner()).thenReturn(vertx);
    when(auditManager.executeDatabaseCleanup(TENANT_ID)).thenReturn(Future.succeededFuture());

    List<List<?>> capturedConstructorArgs = new ArrayList<>();

    try (var pubSubMock = mockStatic(PubSubClientUtils.class);
         var construction = mockConstruction(KafkaAdminClientService.class,
           (mock, ctx) -> {
             capturedConstructorArgs.add(ctx.arguments());
             when(mock.createKafkaTopics(any(), anyString())).thenReturn(Future.succeededFuture());
           })) {

      pubSubMock.when(() -> PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
        .thenReturn(CompletableFuture.completedFuture(true));

      new ModTenantService(auditManager).loadData(new TenantAttributes(), TENANT_ID, Map.of(), context);

      assertThat(capturedConstructorArgs).hasSize(1);
      assertThat(capturedConstructorArgs.get(0)).hasSize(1);
      assertThat(capturedConstructorArgs.get(0).get(0)).isEqualTo(vertx);
    }
  }
}
