package org.folio.dao.management;

import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;
import static org.folio.services.management.YearQuarter.Q4;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.services.management.DatabaseSubPartition;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PartitionDao {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String FULL_TABLE_NAME = "%s.%s";
  private static final String SUB_PARTITION_DATE = "%s-%02d-01";
  private static final String EMPTY_SUB_PARTITIONS_QUERY = """
    SELECT relname
      FROM pg_stat_user_tables
      WHERE schemaname='%s'
      AND relname like '%%audit_p%%q_'
      AND n_live_tup=0;
    """;
  private static final String DROP_TABLES_QUERY = """
    DROP TABLE IF EXISTS %s;
    """;
  private static final String CREATE_SUB_PARTITION_QUERY = """
    CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s');
    
    """;

  private final PostgresClientFactory pgClientFactory;

  public PartitionDao(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  public Future<List<DatabaseSubPartition>> getEmptySubPartitions(String tenantId) {
    LOGGER.debug("getEmptySubPartitions:: tenantId: '{}'", tenantId);
    var schema = convertToPsqlStandard(tenantId);
    var query = EMPTY_SUB_PARTITIONS_QUERY.formatted(schema);
    return pgClientFactory.createInstance(tenantId).execute(query)
      .map(this::mapRowsToDatabaseSubPartitionList);
  }

  public Future<Void> deleteSubPartitions(String tenantId, List<DatabaseSubPartition> subPartitions) {
    if (CollectionUtils.isEmpty(subPartitions)) {
      return Future.succeededFuture();
    }
    LOGGER.debug("deleteSubPartitions:: tenantId: '{}', subPartitions: [{}]", tenantId, subPartitions);
    var schema = convertToPsqlStandard(tenantId);
    var subPartitionsString = subPartitions.stream()
      .map(DatabaseSubPartition::toString)
      .map(tableName -> FULL_TABLE_NAME.formatted(schema, tableName))
      .reduce((a, b) -> a + ", " + b)
      .orElse("");
    var query = DROP_TABLES_QUERY.formatted(subPartitionsString);
    return pgClientFactory.createInstance(tenantId).execute(query)
      .mapEmpty();
  }

  public Future<Void> createSubPartitions(String tenantId, List<DatabaseSubPartition> subPartitions) {
    var schema = convertToPsqlStandard(tenantId);
    var query = subPartitions.stream()
      .map(subPartition -> createSubPartitionQuery(schema, subPartition))
      .reduce((a, b) -> a + b)
      .orElse("");
    return pgClientFactory.createInstance(tenantId).runSqlFile(query)
      .mapEmpty();
  }

  private String createSubPartitionQuery(String schema, DatabaseSubPartition subPartition) {
    var quarter = subPartition.getQuarter();
    var yearTo = Q4.equals(quarter) ? subPartition.getYear() + 1 : subPartition.getYear();
    return CREATE_SUB_PARTITION_QUERY.formatted(
      FULL_TABLE_NAME.formatted(schema, subPartition.toString()),
      FULL_TABLE_NAME.formatted(schema, subPartition.getMainPartition()),
      SUB_PARTITION_DATE.formatted(subPartition.getYear(), quarter.getMonthFrom()),
      SUB_PARTITION_DATE.formatted(yearTo, quarter.getMonthTo())
    );
  }

  private List<DatabaseSubPartition> mapRowsToDatabaseSubPartitionList(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowsToDatabaseSubPartitionList:: Mapping row set to List of DatabaseSubPartition");
    if (rowSet.rowCount() == 0) {
      return new LinkedList<>();
    }
    var subPartitions = new LinkedList<DatabaseSubPartition>();
    rowSet.iterator().forEachRemaining(row ->
      subPartitions.add(mapRowToDatabaseSubPartition(row)));
    LOGGER.debug("mapRowsToDatabaseSubPartitionList:: Mapped row set to List of DatabaseSubPartition");
    return subPartitions;
  }

  private DatabaseSubPartition mapRowToDatabaseSubPartition(Row row) {
    LOGGER.debug("mapRowTooDatabaseSubPartition:: Mapping row to DatabaseSubPartition");
    var s = row.getString(0);
    return DatabaseSubPartition.fromString(s);
  }
}
