package org.folio.dao.marc.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordType;
import org.folio.utils.MockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.folio.utils.EntityUtils.createMarcAuditEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MarcAuditDaoTest {

  private static final String TENANT_ID = "diku";

  @Mock
  private PostgresClientFactory pgClientFactory;

  @Mock
  private RowSet<Row> mockRowSet;

  private MarcAuditDaoImpl marcAuditDao;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    marcAuditDao = new MarcAuditDaoImpl(pgClientFactory);
  }

  @Test
  void shouldSaveMarcAuditEntitySuccessfully() {
    var entity = createMarcAuditEntity();
    var pgClient = mock(PostgresClient.class);
    when(pgClientFactory.createInstance(TENANT_ID)).thenReturn(pgClient);
    when(pgClient.execute(anyString(), any(Tuple.class))).thenReturn(Future.succeededFuture(mockRowSet));

    Future<RowSet<Row>> result = marcAuditDao.save(entity, SourceRecordType.MARC_BIB, TENANT_ID);

    assertTrue(result.succeeded());
    verify(pgClient).execute(anyString(), any(Tuple.class));

    ArgumentCaptor<Tuple> tupleCaptor = ArgumentCaptor.forClass(Tuple.class);
    verify(pgClient).execute(anyString(), tupleCaptor.capture());
    Tuple capturedTuple = tupleCaptor.getValue();

    assertEquals(entity.eventId(), capturedTuple.getUUID(0).toString());
    assertEquals(entity.entityId(), capturedTuple.getUUID(2).toString());
    assertEquals(entity.origin(), capturedTuple.getString(3));
    assertEquals(entity.action(), capturedTuple.getString(4));
    assertEquals(entity.userId(), capturedTuple.getUUID(5).toString());
  }

  @Test
  void shouldRetrieveMarcAuditEntities() {
    var entity = createMarcAuditEntity();
    var entityId = UUID.randomUUID();
    var pgClient = mock(PostgresClient.class);

    when(pgClientFactory.createInstance(TENANT_ID)).thenReturn(pgClient);
    RowSet<Row> rowSetMock = mockRowSet(entity);
    when(pgClient.execute(anyString(), any(Tuple.class))).thenReturn(Future.succeededFuture(rowSetMock));

    Future<List<MarcAuditEntity>> result = marcAuditDao.get(entityId, SourceRecordType.MARC_BIB, TENANT_ID, null, 10);

    assertTrue(result.succeeded());
    assertFalse(result.result().isEmpty());
    verify(pgClient).execute(anyString(), any(Tuple.class));

    var retrievedEntity = result.result().get(0);
    assertEquals(entity.eventId(), retrievedEntity.eventId());
    assertEquals(entity.entityId(), retrievedEntity.entityId());
    assertEquals(entity.userId(), retrievedEntity.userId());
  }

  @Test
  void shouldRetrieveMarcAuditEntitiesWithEventDateFilter() {
    var entity = createMarcAuditEntity();
    var entityId = UUID.randomUUID();
    var eventDate = LocalDateTime.now();
    var pgClient = mock(PostgresClient.class);

    when(pgClientFactory.createInstance(TENANT_ID)).thenReturn(pgClient);
    RowSet<Row> rowSetMock = mockRowSet(entity);
    when(pgClient.execute(anyString(), any(Tuple.class))).thenReturn(Future.succeededFuture(rowSetMock));

    Future<List<MarcAuditEntity>> result = marcAuditDao.get(entityId, SourceRecordType.MARC_BIB, TENANT_ID, eventDate, 5);

    assertTrue(result.succeeded());
    assertFalse(result.result().isEmpty());
    verify(pgClient).execute(anyString(), any(Tuple.class));
  }

  @Test
  void shouldReturnEmptyListWhenNoAuditRecordsExist() {
    var entityId = UUID.randomUUID();
    var pgClient = mock(PostgresClient.class);
    when(pgClientFactory.createInstance(TENANT_ID)).thenReturn(pgClient);
    when(pgClient.execute(anyString(), any(Tuple.class))).thenReturn(Future.succeededFuture(mockRowSet));
    when(mockRowSet.rowCount()).thenReturn(0);

    Future<List<MarcAuditEntity>> result = marcAuditDao.get(entityId, SourceRecordType.MARC_BIB, TENANT_ID, null, 5);

    assertTrue(result.succeeded());
    assertNotNull(result.result());
    assertTrue(result.result().isEmpty());
  }

  private RowSet<Row> mockRowSet(MarcAuditEntity entity) {
    var row = mock(Row.class);
    when(row.getUUID("event_id")).thenReturn(UUID.fromString(entity.eventId()));
    when(row.getUUID("entity_id")).thenReturn(UUID.fromString(entity.entityId()));
    when(row.getUUID("user_id")).thenReturn(UUID.fromString(entity.userId()));
    when(row.getString("origin")).thenReturn(entity.origin());
    when(row.getString("action")).thenReturn(entity.action());
    when(row.getJsonObject("diff")).thenReturn(null);
    when(row.getLocalDateTime("event_date")).thenReturn(entity.eventDate());

    return MockUtils.mockRowSet(row);
  }
}
