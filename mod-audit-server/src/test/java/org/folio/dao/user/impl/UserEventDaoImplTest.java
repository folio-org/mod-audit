package org.folio.dao.user.impl;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createUserAuditEntity;
import static org.folio.utils.MockUtils.mockPostgresExecutionSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Tuple;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class UserEventDaoImplTest {

  @Mock
  PostgresClientFactory postgresClientFactory;
  @Mock
  PostgresClient postgresClient;
  @InjectMocks
  UserEventDaoImpl userEventDao;

  @BeforeEach
  void setUp() {
    lenient().when(postgresClientFactory.createInstance(TENANT_ID)).thenReturn(postgresClient);
    mockPostgresExecutionSuccess(2).when(postgresClient).execute(anyString(), any(Tuple.class), any());
  }

  @Test
  void shouldSaveSuccessfully(VertxTestContext ctx) {
    var entity = createUserAuditEntity();

    userEventDao.save(entity, TENANT_ID)
      .onComplete(ctx.succeeding(result -> ctx.completeNow()));

    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldHandleExceptionOnSave(VertxTestContext ctx) {
    var entity = createUserAuditEntity();

    mockPostgresExecutionSuccess(2)
      .doThrow(new IllegalStateException("Error"))
      .when(postgresClient).execute(anyString(), any(Tuple.class), any());

    userEventDao.save(entity, TENANT_ID)
      .onComplete(ctx.succeeding(result ->
        userEventDao.save(entity, TENANT_ID)
          .onComplete(re -> {
            assertTrue(re.failed());
            assertInstanceOf(IllegalStateException.class, re.cause());
            assertEquals("Error", re.cause().getMessage());
            ctx.completeNow();
          })
      ));
    verify(postgresClientFactory, times(2)).createInstance(TENANT_ID);
  }

  @Test
  void shouldDeleteByUserId(VertxTestContext ctx) {
    var userId = createUserAuditEntity().userId();

    doReturn(Future.succeededFuture())
      .when(postgresClient).execute(anyString(), any(Tuple.class));

    userEventDao.deleteByUserId(userId, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(postgresClient, times(1)).execute(anyString(), any(Tuple.class));
        ctx.completeNow();
      }));
  }

  @Test
  void shouldReturnCorrectTableName() {
    assertEquals("user_audit", userEventDao.tableName());
  }
}
