package org.folio.dao.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import org.folio.CopilotGenerated;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.management.DatabaseSubPartition;
import org.folio.services.management.YearQuarter;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.MockUtils;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@CopilotGenerated(partiallyGenerated = true)
@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class PartitionDaoTest {

  @Mock
  PostgresClientFactory postgresClientFactory;
  @Mock
  PostgresClient postgresClient;
  @InjectMocks
  PartitionDao partitionDao;

  @BeforeEach
  void setUp() {
    lenient().when(postgresClientFactory.createInstance(TENANT_ID)).thenReturn(postgresClient);
  }

  @Test
  void shouldGetEmptySubPartitions(VertxTestContext ctx) {
    var subPartition = subPartition();
    var rowSet = mockRowSet(subPartition);

    doReturn(Future.succeededFuture(rowSet))
      .when(postgresClient).execute(anyString());

    partitionDao.getEmptySubPartitions(TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        assertEquals(1, result.size());
        assertEquals(subPartition.toString(), result.get(0).toString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldDeleteSubPartitions(VertxTestContext ctx) {
    var subPartition = subPartition();

    doReturn(Future.succeededFuture())
      .when(postgresClient).execute(anyString());

    partitionDao.deleteSubPartitions(TENANT_ID, List.of(subPartition))
      .onComplete(ctx.succeeding(result -> {
        verify(postgresClient, times(1)).execute(anyString());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldHandleNullSubPartitionsOnDelete(VertxTestContext ctx) {
    partitionDao.deleteSubPartitions(TENANT_ID, null)
      .onComplete(ctx.succeeding(result -> {
        verifyNoInteractions(postgresClient);
        ctx.completeNow();
      }));
  }

  @Test
  void shouldCreateSubPartitions(VertxTestContext ctx) {
    var subPartitions = List.of(
      subPartition(YearQuarter.Q4, 2024),
      subPartition(YearQuarter.Q1, 2025),
      subPartition(YearQuarter.Q2, 2025),
      subPartition(YearQuarter.Q3, 2025)
    );
    var expectedRanges = List.of(
      "FROM ('2024-10-01') TO ('2025-01-01')",
      "FROM ('2025-01-01') TO ('2025-04-01')",
      "FROM ('2025-04-01') TO ('2025-07-01')",
      "FROM ('2025-07-01') TO ('2025-10-01')"
    );

    doReturn(Future.succeededFuture())
      .when(postgresClient).runSqlFile(anyString());

    partitionDao.createSubPartitions(TENANT_ID, subPartitions)
      .onComplete(ctx.succeeding(result -> {
        var captor = ArgumentCaptor.forClass(String.class);
        verify(postgresClient, times(1)).runSqlFile(captor.capture());
        var query = captor.getValue();
        assertThat(query)
          .contains(subPartitions.stream().map(DatabaseSubPartition::toString).toList())
          .contains(expectedRanges);
        ctx.completeNow();
      }));
  }

  private DatabaseSubPartition subPartition() {
    return subPartition(0);
  }

  private DatabaseSubPartition subPartition(int mainPartitionNumber) {
    return DatabaseSubPartition.fromString("audit_p" + mainPartitionNumber + "_2024_q4");
  }

  private DatabaseSubPartition subPartition(YearQuarter quarter, int year) {
    return new DatabaseSubPartition("audit", 0, year, quarter);
  }

  private RowSet<Row> mockRowSet(DatabaseSubPartition subPartition) {
    var row = mock(Row.class);
    when(row.getString(0)).thenReturn(subPartition.toString());
    return MockUtils.mockRowSet(row);
  }
}