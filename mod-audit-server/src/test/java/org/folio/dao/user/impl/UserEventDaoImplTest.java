package org.folio.dao.user.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createUserAuditEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.sql.Timestamp;
import java.util.Set;
import org.folio.dao.user.UserAuditConstants;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    lenient().doReturn(Future.succeededFuture(mock(RowSet.class)))
      .when(postgresClient).execute(anyString(), any(Tuple.class));
  }

  @Test
  void shouldSaveSuccessfully(VertxTestContext ctx) {
    var entity = createUserAuditEntity();

    userEventDao.save(entity, TENANT_ID)
      .onComplete(ctx.succeeding(result -> ctx.completeNow()));

    verify(postgresClientFactory).createInstance(TENANT_ID);
  }

  @Test
  void shouldHandleExceptionOnSave(VertxTestContext ctx) {
    var entity = createUserAuditEntity();

    doReturn(Future.succeededFuture(mock(RowSet.class)))
      .doReturn(Future.failedFuture(new IllegalStateException("Error")))
      .when(postgresClient).execute(anyString(), any(Tuple.class));

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
        verify(postgresClient).execute(anyString(), any(Tuple.class));
        ctx.completeNow();
      }));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldDeleteAll(VertxTestContext ctx) {
    var conn = mock(Conn.class);
    when(conn.execute(anyString())).thenReturn(Future.succeededFuture(mock(RowSet.class)));

    userEventDao.deleteAll(conn, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(conn).execute(anyString());
        ctx.completeNow();
      }));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldAnonymizeAll(VertxTestContext ctx) {
    var conn = mock(Conn.class);
    when(conn.execute(anyString())).thenReturn(Future.succeededFuture(mock(RowSet.class)));

    userEventDao.anonymizeAll(conn, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(conn).execute(anyString());
        ctx.completeNow();
      }));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldDeleteEmptyUpdateRecords(VertxTestContext ctx) {
    var conn = mock(Conn.class);
    when(conn.execute(anyString())).thenReturn(Future.succeededFuture(mock(RowSet.class)));

    userEventDao.deleteEmptyUpdateRecords(conn, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(conn).execute(anyString());
        ctx.completeNow();
      }));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldDeleteMetadataOnlyUpdateRecords(VertxTestContext ctx) {
    var conn = mock(Conn.class);
    when(conn.execute(anyString(), any(Tuple.class))).thenReturn(Future.succeededFuture(mock(RowSet.class)));

    userEventDao.deleteMetadataOnlyUpdateRecords(conn, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        var tupleCaptor = ArgumentCaptor.forClass(Tuple.class);
        verify(conn).execute(anyString(), tupleCaptor.capture());
        var boundPaths = Set.of((String[]) tupleCaptor.getValue().getArrayOfStrings(0));
        assertThat(boundPaths).containsExactlyInAnyOrderElementsOf(UserAuditConstants.INTERNAL_METADATA_FIELD_PATHS);
        ctx.completeNow();
      }));
  }

  @Test
  void shouldDeleteOlderThanDate(VertxTestContext ctx) {
    var tenDaysAgo = new Timestamp(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000);

    doReturn(Future.succeededFuture())
      .when(postgresClient).execute(anyString(), any(Tuple.class));

    userEventDao.deleteOlderThanDate(tenDaysAgo, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(postgresClient).execute(anyString(), any(Tuple.class));
        ctx.completeNow();
      }));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldDeleteOlderThanDateWithConn(VertxTestContext ctx) {
    var conn = mock(Conn.class);
    var tenDaysAgo = new Timestamp(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000);

    when(conn.execute(anyString(), any(Tuple.class))).thenReturn(Future.succeededFuture(mock(RowSet.class)));

    userEventDao.deleteOlderThanDate(tenDaysAgo, conn, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        verify(conn).execute(anyString(), any(Tuple.class));
        ctx.completeNow();
      }));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldExcludeFieldsFromAll(VertxTestContext ctx) {
    var conn = mock(Conn.class);
    var paths = Set.of("personal.email", "barcode");
    when(conn.execute(anyString(), any(Tuple.class))).thenReturn(Future.succeededFuture(mock(RowSet.class)));

    userEventDao.excludeFieldsFromAll(paths, conn, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        var tupleCaptor = ArgumentCaptor.forClass(Tuple.class);
        verify(conn, times(1)).execute(anyString(), tupleCaptor.capture());
        var boundPaths = Set.of((String[]) tupleCaptor.getValue().getArrayOfStrings(0));
        assertThat(boundPaths).containsExactlyInAnyOrder("personal.email", "barcode");
        ctx.completeNow();
      }));
  }


  @Test
  void shouldReturnFailedFutureWhenSynchronousExceptionOccurs() {
    var entity = createUserAuditEntity();
    when(postgresClientFactory.createInstance(TENANT_ID)).thenThrow(new RuntimeException("db error"));

    var result = userEventDao.save(entity, TENANT_ID);

    assertTrue(result.failed());
    assertInstanceOf(RuntimeException.class, result.cause());
    assertEquals("db error", result.cause().getMessage());
  }

  @Test
  void shouldReturnCorrectTableName() {
    assertEquals("user_audit", userEventDao.tableName());
  }
}
