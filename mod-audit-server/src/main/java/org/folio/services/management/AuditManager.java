package org.folio.services.management;

import io.vertx.core.Future;
import java.sql.Timestamp;
import java.util.function.BiFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.SettingGroup;
import org.folio.services.configuration.SettingKey;
import org.folio.services.inventory.InventoryEventService;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Service;

@Service
public class AuditManager {

  private static final Logger LOGGER = LogManager.getLogger();

  private final ConfigurationService configurationService;
  private final InventoryEventService inventoryService;
  private final MarcAuditService marcService;
  private final PartitionService partitionService;

  public AuditManager(ConfigurationService configurationService, InventoryEventService inventoryService,
                      MarcAuditService marcService, PartitionService partitionService) {
    this.configurationService = configurationService;
    this.inventoryService = inventoryService;
    this.marcService = marcService;
    this.partitionService = partitionService;
  }

  public Future<Void> executeDatabaseCleanup(String tenantId) {
    var currentTime = System.currentTimeMillis();
    return deleteExpiredRecords(tenantId, currentTime);
  }

  private Future<Void> deleteExpiredRecords(String tenantId, long currentTime) {
    return Future.all(
      deleteExpiredRecordsForSettingGroup(tenantId, SettingGroup.INVENTORY, currentTime,
        (tenant, expireOlderThan) -> Future.all(
          inventoryService.expireRecords(tenant, expireOlderThan),
          marcService.expireRecords(tenant, expireOlderThan, SourceRecordType.MARC_BIB)).mapEmpty()),
      deleteExpiredRecordsForSettingGroup(tenantId, SettingGroup.AUTHORITY, currentTime,
        (tenant, expireOlderThan) ->
          marcService.expireRecords(tenant, expireOlderThan, SourceRecordType.MARC_AUTHORITY)),
      partitionService.cleanUpAndCreateSubPartitions(tenantId)
    ).mapEmpty();
  }

  private Future<Void> deleteExpiredRecordsForSettingGroup(
    String tenantId, SettingGroup settingGroup, long currentTime,
    BiFunction<String, Timestamp, Future<Void>> expirationFutureSupplier) {
    return configurationService.getAllSettingsByGroupId(settingGroup.getId(), tenantId)
      .compose(settingCollection -> {
        var auditEnabled = settingCollection.getSettings().stream()
          .filter(setting -> SettingKey.ENABLED.getValue().equals(setting.getKey()))
          .findFirst()
          .orElse(null);
        if (auditEnabled == null || !((boolean) auditEnabled.getValue())) {
          LOGGER.debug("deleteExpiredRecordsForSettingGroup:: Audit is disabled for tenant [tenantId: {}, settingGroup {}]", tenantId, settingGroup.getId());
          return Future.succeededFuture();
        }

        var retentionPeriod = settingCollection.getSettings().stream()
          .filter(setting -> SettingKey.RETENTION_PERIOD.getValue().equals(setting.getKey()))
          .findFirst()
          .map(setting -> (int) setting.getValue())
          .orElse(null);
        if (retentionPeriod == null || retentionPeriod < 1) {
          LOGGER.debug("deleteExpiredRecordsForSettingGroup:: Audit expiration is disabled for tenant [tenantId: {}, settingGroup {}]", tenantId, settingGroup.getId());
          return Future.succeededFuture();
        }

        var expireOlderThan = getExpireOlderThan(currentTime, retentionPeriod);
        LOGGER.info(
          "deleteExpiredRecordsForSettingGroup:: Deleting expired records for [tenantId: {}, settingGroup: {}, retentionPeriod: {}, expireOlderThan: {}]",
          tenantId, settingGroup.getId(), retentionPeriod, expireOlderThan);
        return expirationFutureSupplier.apply(tenantId, expireOlderThan);
      });
  }

  /**
   * Calculate the latest timestamp that should be kept based on the current time and retention period.
   *
   * @param currentTime current time in milliseconds
   * @param retentionPeriod retention period in days
   * @return Timestamp
   * */
  private Timestamp getExpireOlderThan(long currentTime, int retentionPeriod) {
    return new Timestamp(currentTime - retentionPeriod * 24 * 60 * 60 * 1000L);
  }
}
