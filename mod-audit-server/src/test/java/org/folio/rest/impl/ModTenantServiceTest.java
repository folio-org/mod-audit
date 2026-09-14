package org.folio.rest.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response;
import org.apache.kafka.common.errors.TopicExistsException;
import org.folio.kafka.services.KafkaAdminClientService;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.resource.Tenant.PostTenantResponse;
import org.folio.services.management.AuditManager;
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
  private static final String OKAPI_TENANT_HEADER = "X-Okapi-Tenant";

  @Mock
  private AuditManager auditManager;
  @Mock
  private Vertx vertx;
  @Mock
  private Context context;

  private ModTenantService service;

  @BeforeEach
  void setUp() {
    service = new ModTenantService(auditManager);
  }

  @Test
  void postTenantKeepsRmbValidation() throws NoSuchMethodException {
    var postTenant = ModTenantService.class.getMethod("postTenant",
      TenantAttributes.class, Map.class, Handler.class, Context.class);

    assertThat(postTenant.isAnnotationPresent(Validate.class)).isTrue();
  }

  @Test
  void postTenantCreatesKafkaTopicsAfterTenantSyncWhenEnabling() throws InterruptedException {
    var tenantResponse = PostTenantResponse.respond204();
    var tenantService = new TestModTenantService(auditManager, Future.succeededFuture((Response) tenantResponse));
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    when(context.owner()).thenReturn(vertx);

    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.succeededFuture()))) {
      var result = postTenant(tenantService, attributes);

      assertThat(result.succeeded()).isTrue();
      assertThat(result.result()).isSameAs(tenantResponse);
      verify(mocked.constructed().get(0)).createKafkaTopics(any(), eq(TENANT_ID));
    }
  }

  @Test
  void postTenantDeletesKafkaTopicsAfterTenantSyncWhenDisablingWithPurge() throws InterruptedException {
    var tenantResponse = PostTenantResponse.respond204();
    var tenantService = new TestModTenantService(auditManager, Future.succeededFuture((Response) tenantResponse));
    var attributes = new TenantAttributes().withModuleFrom("1.0.0").withPurge(Boolean.TRUE);
    when(context.owner()).thenReturn(vertx);

    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.deleteKafkaTopics(any(), any())).thenReturn(Future.succeededFuture()))) {
      var result = postTenant(tenantService, attributes);

      assertThat(result.succeeded()).isTrue();
      assertThat(result.result()).isSameAs(tenantResponse);
      assertThat(tenantService.postTenantSyncAttributes.getModuleTo()).isNull();
      verify(mocked.constructed().get(0)).deleteKafkaTopics(any(), eq(TENANT_ID));
    }
  }

  @Test
  void postTenantDeletesKafkaTopicsWhenPurgingWithModuleToConfigured() throws InterruptedException {
    var tenantResponse = PostTenantResponse.respond204();
    var tenantService = new TestModTenantService(auditManager, Future.succeededFuture((Response) tenantResponse));
    var attributes = new TenantAttributes().withModuleTo("1.0.0").withPurge(Boolean.TRUE);
    when(context.owner()).thenReturn(vertx);

    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.deleteKafkaTopics(any(), any())).thenReturn(Future.succeededFuture()))) {
      var result = postTenant(tenantService, attributes);

      assertThat(result.succeeded()).isTrue();
      assertThat(result.result()).isSameAs(tenantResponse);
      verify(mocked.constructed().get(0)).deleteKafkaTopics(any(), eq(TENANT_ID));
      verify(mocked.constructed().get(0), never()).createKafkaTopics(any(), any());
    }
  }

  @Test
  void postTenantDoesNotDeleteKafkaTopicsWhenDisablingWithoutPurge() throws InterruptedException {
    var tenantResponse = PostTenantResponse.respond204();
    var tenantService = new TestModTenantService(auditManager, Future.succeededFuture((Response) tenantResponse));
    var attributes = new TenantAttributes().withModuleFrom("1.0.0");
    when(context.owner()).thenReturn(vertx);

    try (var mocked = mockConstruction(KafkaAdminClientService.class)) {
      var result = postTenant(tenantService, attributes);

      assertThat(result.succeeded()).isTrue();
      assertThat(result.result()).isSameAs(tenantResponse);
      assertThat(mocked.constructed()).isEmpty();
    }
  }

  @Test
  void postTenantRespondsWithServerErrorWhenKafkaTopicsCreationFails() throws InterruptedException {
    var tenantService = new TestModTenantService(auditManager,
      Future.succeededFuture((Response) PostTenantResponse.respond204()));
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    when(context.owner()).thenReturn(vertx);

    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.failedFuture(new RuntimeException("boom"))))) {
      var result = postTenant(tenantService, attributes);

      assertThat(result.succeeded()).isTrue();
      assertThat(result.result().getStatus()).isEqualTo(500);
      verify(mocked.constructed().get(0)).createKafkaTopics(any(), eq(TENANT_ID));
    }
  }

  @Test
  void postTenantDoesNotUpdateKafkaTopicsWhenTenantSyncFails() throws InterruptedException {
    var failure = new RuntimeException("tenant sync failed");
    var tenantService = new TestModTenantService(auditManager, Future.failedFuture(failure));
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    when(context.owner()).thenReturn(vertx);

    try (var mocked = mockConstruction(KafkaAdminClientService.class)) {
      var result = postTenant(tenantService, attributes);

      assertThat(result.failed()).isTrue();
      assertThat(result.cause()).isSameAs(failure);
      assertThat(mocked.constructed()).isEmpty();
    }
  }

  @Test
  void postTenantPassesBlankModuleToUnchangedToRmb() throws InterruptedException {
    var failure = new RuntimeException("tenant sync failed");
    var tenantService = new TestModTenantService(auditManager, Future.failedFuture(failure));
    var attributes = new TenantAttributes().withModuleFrom("1.0.0").withModuleTo("");
    when(context.owner()).thenReturn(vertx);

    var result = postTenant(tenantService, attributes);

    assertThat(result.failed()).isTrue();
    assertThat(tenantService.postTenantSyncAttributes.getModuleTo()).isEmpty();
  }

  @Test
  void createTopicsIfEnabledCreatesTopicsWhenModuleEnabled() {
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.succeededFuture()))) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertThat(result.succeeded()).isTrue();
      verify(mocked.constructed().get(0)).createKafkaTopics(any(), eq(TENANT_ID));
    }
  }

  @Test
  void createTopicsIfEnabledDoesNothingWhenModuleToIsNull() {
    var attributes = new TenantAttributes();
    try (var mocked = mockConstruction(KafkaAdminClientService.class)) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertThat(result.succeeded()).isTrue();
      assertThat(mocked.constructed()).isEmpty();
    }
  }

  @Test
  void createTopicsIfEnabledSucceedsWhenTopicsAlreadyExist() {
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    var topicExistsException = new TopicExistsException("already exists");
    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.failedFuture(topicExistsException)))) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertThat(result.succeeded()).isTrue();
    }
  }

  @Test
  void createTopicsIfEnabledFailsOnUnexpectedError() {
    var attributes = new TenantAttributes().withModuleTo("1.0.0");
    try (var mocked = mockConstruction(KafkaAdminClientService.class, (mock, ctx) ->
        when(mock.createKafkaTopics(any(), any())).thenReturn(Future.failedFuture(new RuntimeException("boom"))))) {
      Future<Void> result = service.createTopicsIfEnabled(attributes, vertx, TENANT_ID);
      assertThat(result.failed()).isTrue();
    }
  }

  @Test
  void loadDataRunsDatabaseCleanup() throws InterruptedException {
    var attributes = new TenantAttributes(); // moduleTo == null: no topic creation
    when(auditManager.executeDatabaseCleanup(TENANT_ID)).thenReturn(Future.succeededFuture());

    var latch = new CountDownLatch(1);
    service.loadData(attributes, TENANT_ID, Map.of(), context)
      .onComplete(ar -> latch.countDown());

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    verify(auditManager).executeDatabaseCleanup(TENANT_ID);
  }

  private AsyncResult<Response> postTenant(ModTenantService tenantService, TenantAttributes attributes)
    throws InterruptedException {

    var resultReference = new AtomicReference<AsyncResult<Response>>();
    var latch = new CountDownLatch(1);
    tenantService.postTenant(attributes, Map.of(OKAPI_TENANT_HEADER, TENANT_ID), ar -> {
      resultReference.set(ar);
      latch.countDown();
    }, context);

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    return resultReference.get();
  }

  private static final class TestModTenantService extends ModTenantService {

    private final Future<Response> postTenantSyncResult;
    private TenantAttributes postTenantSyncAttributes;

    private TestModTenantService(AuditManager auditManager, Future<Response> postTenantSyncResult) {
      super(auditManager);
      this.postTenantSyncResult = postTenantSyncResult;
    }

    @Override
    public Future<Response> postTenantSync(TenantAttributes attributes, Map<String, String> headers, Context context) {
      postTenantSyncAttributes = attributes;
      return postTenantSyncResult;
    }
  }
}
