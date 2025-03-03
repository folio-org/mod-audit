package org.folio.services.management;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import java.sql.Timestamp;
import java.util.List;
import org.folio.CopilotGenerated;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.model.SettingCollection;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.SettingGroup;
import org.folio.services.configuration.SettingKey;
import org.folio.services.inventory.InventoryEventService;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.SourceRecordType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@CopilotGenerated
@ExtendWith(MockitoExtension.class)
class AuditManagerTest {

  @Mock
  private ConfigurationService configurationService;
  @Mock
  private InventoryEventService inventoryService;
  @Mock
  private MarcAuditService marcService;
  @Mock
  private PartitionService partitionService;

  private AuditManager auditManager;

  @BeforeEach
  void setUp() {
    auditManager = new AuditManager(configurationService, inventoryService, marcService, partitionService);
  }

  @Test
  void testExecuteDatabaseCleanupWithEnabledSettingsAndPositiveRetentionPeriod() {
    var currentTime = System.currentTimeMillis();

    var enabledSetting = new Setting().withKey(SettingKey.ENABLED.getValue()).withValue(true);
    var retentionSetting = new Setting().withKey(SettingKey.RETENTION_PERIOD.getValue()).withValue(10);

    var settingCollection = new SettingCollection().withSettings(List.of(enabledSetting, retentionSetting));

    when(configurationService.getAllSettingsByGroupId(SettingGroup.INVENTORY.getId(), TENANT_ID))
      .thenReturn(Future.succeededFuture(settingCollection));
    when(configurationService.getAllSettingsByGroupId(SettingGroup.AUTHORITY.getId(), TENANT_ID))
      .thenReturn(Future.succeededFuture(settingCollection));

    when(inventoryService.expireRecords(eq(TENANT_ID), any(Timestamp.class)))
      .thenReturn(Future.succeededFuture());
    when(marcService.expireRecords(eq(TENANT_ID), any(Timestamp.class), eq(SourceRecordType.MARC_BIB)))
      .thenReturn(Future.succeededFuture());
    when(marcService.expireRecords(eq(TENANT_ID), any(Timestamp.class), eq(SourceRecordType.MARC_AUTHORITY)))
      .thenReturn(Future.succeededFuture());
    when(partitionService.cleanUpAndCreateSubPartitions(TENANT_ID))
      .thenReturn(Future.succeededFuture());

    var result = auditManager.executeDatabaseCleanup(TENANT_ID);
    result.onComplete(ar -> assertTrue(ar.succeeded()));

    var timestampCaptor = ArgumentCaptor.forClass(Timestamp.class);

    verify(inventoryService, times(1)).expireRecords(eq(TENANT_ID), timestampCaptor.capture());
    verify(marcService, times(1)).expireRecords(eq(TENANT_ID), timestampCaptor.capture(), eq(SourceRecordType.MARC_BIB));
    verify(marcService, times(1)).expireRecords(eq(TENANT_ID), timestampCaptor.capture(), eq(SourceRecordType.MARC_AUTHORITY));
    verify(partitionService).cleanUpAndCreateSubPartitions(TENANT_ID);

    var capturedTimestamps = timestampCaptor.getAllValues();
    capturedTimestamps.forEach(timestamp -> {
      assertEquals(new Timestamp(currentTime - 10L * 24 * 60 * 60 * 1000).toLocalDateTime().toLocalDate(),
        timestamp.toLocalDateTime().toLocalDate());
    });
  }

  @Test
  void testExecuteDatabaseCleanupWithDisabledAuthorityAndNegativeInventoryRetentionPeriod() {
    var disabledSetting = new Setting().withKey(SettingKey.ENABLED.getValue()).withValue(false);
    var negativeRetentionSetting = new Setting().withKey(SettingKey.RETENTION_PERIOD.getValue()).withValue(-1);

    var authoritySettingCollection = new SettingCollection().withSettings(List.of(disabledSetting));
    var inventorySettingCollection = new SettingCollection().withSettings(List.of(negativeRetentionSetting));

    when(configurationService.getAllSettingsByGroupId(SettingGroup.AUTHORITY.getId(), TENANT_ID))
      .thenReturn(Future.succeededFuture(authoritySettingCollection));
    when(configurationService.getAllSettingsByGroupId(SettingGroup.INVENTORY.getId(), TENANT_ID))
      .thenReturn(Future.succeededFuture(inventorySettingCollection));
    when(partitionService.cleanUpAndCreateSubPartitions(TENANT_ID))
      .thenReturn(Future.succeededFuture());

    var result = auditManager.executeDatabaseCleanup(TENANT_ID);
    result.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(inventoryService, never()).expireRecords(eq(TENANT_ID), any(Timestamp.class));
    verify(marcService, never()).expireRecords(eq(TENANT_ID), any(Timestamp.class), eq(SourceRecordType.MARC_BIB));
    verify(marcService, never()).expireRecords(eq(TENANT_ID), any(Timestamp.class), eq(SourceRecordType.MARC_AUTHORITY));
    verify(partitionService).cleanUpAndCreateSubPartitions(TENANT_ID);
  }
}