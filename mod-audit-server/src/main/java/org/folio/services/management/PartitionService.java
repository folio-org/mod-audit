package org.folio.services.management;

import io.vertx.core.Future;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.inventory.InventoryEventDao;
import org.folio.dao.management.PartitionDao;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.Setting;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Service;

@Service
public class PartitionService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final PartitionDao partitionDao;
  private final List<InventoryEventDao> inventoryEventDaoList;
  private final MarcAuditDao marcAuditDao;
  private final ConfigurationService configurationService;

  public PartitionService(PartitionDao partitionDao, List<InventoryEventDao> inventoryEventDaoList,
                          MarcAuditDao marcAuditDao, ConfigurationService configurationService) {
    this.partitionDao = partitionDao;
    this.inventoryEventDaoList = inventoryEventDaoList;
    this.marcAuditDao = marcAuditDao;
    this.configurationService = configurationService;
  }

  public Future<Void> cleanUpAndCreateSubPartitions(String tenantId) {
    var now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());

    return partitionDao.getEmptySubPartitions(tenantId)
      .compose(subPartitions -> {
        var subPartitionsGroupedByDate = subPartitions.stream()
          .filter(subPartition -> !subPartition.isCurrent(now))
          .collect(Collectors.groupingBy(subPartition -> subPartition.isBefore(now)));
        return partitionDao.deleteSubPartitions(tenantId, subPartitionsGroupedByDate.get(true))
          .compose(v -> createNewSubPartitions(tenantId, now, subPartitionsGroupedByDate.get(false)));
      });
  }

  private Future<Void> createNewSubPartitions(String tenantId, LocalDateTime now, List<DatabaseSubPartition> existingSubPartitions) {
    var currentQuarter = YearQuarter.current(now);
    var nextQuarter = YearQuarter.next(now);
    var year = currentQuarter.getValue() < nextQuarter.getValue() ? now.getYear() : now.getYear() + 1;

    var inventoryTableNames = inventoryTableNames();
    var authorityTableNames = authorityTableNames();
    if (existingSubPartitions == null) {
      existingSubPartitions = new LinkedList<>();
    }

    return Future.all(
      createSubPartitionsForSetting(tenantId, Setting.INVENTORY_RECORDS_ENABLED, year, nextQuarter, inventoryTableNames, existingSubPartitions),
      createSubPartitionsForSetting(tenantId, Setting.AUTHORITY_RECORDS_ENABLED, year, nextQuarter, authorityTableNames, existingSubPartitions)
    ).mapEmpty();
  }

  /**
   * Verify that feature is enabled for tenant and create sub partitions for tables which doesn't have them for the next quarter
   * */
  private Future<Void> createSubPartitionsForSetting(String tenantId, Setting setting, int year, YearQuarter nextQuarter,
                                                     List<String> tableNames, List<DatabaseSubPartition> existingSubPartitions) {
    return configurationService.getSetting(setting, tenantId)
      .compose(settingValue -> {
        if (settingValue == null || !((boolean) settingValue.getValue())) {
          LOGGER.debug("createPartitionsForSetting:: Audit is disabled for tenant [tenantId: {}, settingGroup {}]",
            tenantId, setting.getGroup().getId());
          return Future.succeededFuture();
        }

        var tableNamesWithoutSubPartitions = tableNames.stream()
          .filter(tableName -> existingSubPartitions.stream()
            .noneMatch(subPartition -> subPartition.getTable().equals(tableName)))
          .toList();

        var newSubPartitions = tableNamesWithoutSubPartitions.stream()
          .map(tableName -> subPartitionsForTable(tableName, year, nextQuarter))
          .flatMap(List::stream)
          .toList();

        if (newSubPartitions.isEmpty()) {
          LOGGER.debug("createPartitionsForSetting:: No new partitions to create for tenant [tenantId: {}, settingGroup {}]",
            tenantId, setting.getGroup().getId());
          return Future.succeededFuture();
        }

        return partitionDao.createSubPartitions(tenantId, newSubPartitions);
      });
  }

  /**
   * Returns 8 sub partitions, 1 for each parent partition
   * */
  private List<DatabaseSubPartition> subPartitionsForTable(String tableName, int year, YearQuarter nextQuarter) {
    return Stream.iterate(0, i -> i + 1)
      .map(i -> new DatabaseSubPartition(tableName, i, year, nextQuarter))
      .limit(8)
      .toList();
  }

  private List<String> inventoryTableNames() {
    var inventoryTableNames = inventoryEventDaoList.stream()
      .map(InventoryEventDao::tableName)
      .toList();
    var tableNames = new LinkedList<>(inventoryTableNames);
    tableNames.add(marcAuditDao.tableName(SourceRecordType.MARC_BIB));
    return tableNames;
  }

  private List<String> authorityTableNames() {
    return List.of(marcAuditDao.tableName(SourceRecordType.MARC_AUTHORITY));
  }
}
