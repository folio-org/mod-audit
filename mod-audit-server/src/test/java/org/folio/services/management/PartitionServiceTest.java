package org.folio.services.management;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import java.time.LocalDateTime;
import java.util.List;
import org.folio.CopilotGenerated;
import org.folio.dao.inventory.InventoryEventDao;
import org.folio.dao.inventory.impl.InstanceEventDao;
import org.folio.dao.management.PartitionDao;
import org.folio.dao.marc.impl.MarcAuditDaoImpl;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.Setting;
import org.folio.services.configuration.SettingKey;
import org.folio.util.marc.SourceRecordType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@CopilotGenerated(partiallyGenerated = true)
@ExtendWith(MockitoExtension.class)
class PartitionServiceTest {

  @Mock
  private PartitionDao partitionDao;
  @Mock
  private MarcAuditDaoImpl marcAuditDao;
  @Mock
  private ConfigurationService configurationService;

  private List<InventoryEventDao> inventoryEventDaoList;
  private PartitionService partitionService;

  @BeforeEach
  void setUp() {
    var instanceDao = Mockito.mock(InstanceEventDao.class);
    when(instanceDao.tableName()).thenCallRealMethod();
    when(instanceDao.resourceType()).thenCallRealMethod();
    when(marcAuditDao.tableName(any())).thenCallRealMethod();
    inventoryEventDaoList = List.of(instanceDao);
    partitionService = new PartitionService(partitionDao, inventoryEventDaoList, marcAuditDao, configurationService);
  }

  @Test
  void testCleanUpAndCreateSubPartitionsWithDisabledSetting() {
    var now = LocalDateTime.now();
    var currentQuarter = YearQuarter.current(now);
    var previousQuarter = currentQuarter.getValue() == 1 ? YearQuarter.fromValue(4) : YearQuarter.fromValue(currentQuarter.getValue() - 1);
    var yearForPreviousQuarter = currentQuarter.getValue() > previousQuarter.getValue() ? now.getYear() : now.getYear() - 1;
    var nextQuarter = YearQuarter.next(now);
    var yearForNextQuarter = currentQuarter.getValue() < nextQuarter.getValue() ? now.getYear() : now.getYear() + 1;

    // Mocking the configurationService to return the appropriate settings
    when(configurationService.getSetting(Setting.INVENTORY_RECORDS_ENABLED, TENANT_ID))
      .thenReturn(Future.succeededFuture(enabledSetting(true)));
    when(configurationService.getSetting(Setting.AUTHORITY_RECORDS_ENABLED, TENANT_ID))
      .thenReturn(Future.succeededFuture(enabledSetting(false)));

    // Mocking partitionDao.getEmptySubPartitions to return specific partitions
    var instanceTableName = inventoryEventDaoList.getFirst().tableName();
    var marcBibTableName = marcAuditDao.tableName(SourceRecordType.MARC_BIB);
    var marcAuthorityTableName = marcAuditDao.tableName(SourceRecordType.MARC_AUTHORITY);

    var emptySubPartitions = List.of(
      new DatabaseSubPartition(instanceTableName, 0, yearForPreviousQuarter, previousQuarter),
      new DatabaseSubPartition(instanceTableName, 0, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 0, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 0, yearForPreviousQuarter, previousQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 0, yearForPreviousQuarter, previousQuarter)
    );

    when(partitionDao.getEmptySubPartitions(TENANT_ID))
      .thenReturn(Future.succeededFuture(emptySubPartitions));
    when(partitionDao.deleteSubPartitions(eq(TENANT_ID), any()))
      .thenReturn(Future.succeededFuture());
    when(partitionDao.createSubPartitions(eq(TENANT_ID), any()))
      .thenReturn(Future.succeededFuture());

    // Executing the method under test
    var result = partitionService.cleanUpAndCreateSubPartitions(TENANT_ID);
    result.onComplete(ar -> assertTrue(ar.succeeded()));

    // Verifying interactions with partitionDao
    verify(partitionDao, times(1)).getEmptySubPartitions(TENANT_ID);
    verify(partitionDao, times(1)).deleteSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(instanceTableName, 0, yearForPreviousQuarter, previousQuarter),
      new DatabaseSubPartition(marcBibTableName, 0, yearForPreviousQuarter, previousQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 0, yearForPreviousQuarter, previousQuarter)
    ));

    // Verifies creation of current quarter subpartitions
    verify(partitionDao, times(1)).createSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(marcBibTableName, 0, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 1, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 2, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 3, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 4, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 5, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 6, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 7, now.getYear(), currentQuarter)
    ));

    // Verifies creation of next quarter subpartitions
    verify(partitionDao, times(1)).createSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(marcBibTableName, 0, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 1, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 2, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 3, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 4, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 5, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 6, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 7, yearForNextQuarter, nextQuarter)
    ));

    verifyNoMoreInteractions(partitionDao);
  }

  @Test
  void testCleanUpAndCreateSubPartitionsWithCurrentQuarterPartitions() {
    var now = LocalDateTime.now();
    var currentQuarter = YearQuarter.current(now);
    var nextQuarter = YearQuarter.next(now);
    var yearForNextQuarter = currentQuarter.getValue() < nextQuarter.getValue() ? now.getYear() : now.getYear() + 1;

    when(configurationService.getSetting(Setting.INVENTORY_RECORDS_ENABLED, TENANT_ID))
      .thenReturn(Future.succeededFuture(enabledSetting(true)));
    when(configurationService.getSetting(Setting.AUTHORITY_RECORDS_ENABLED, TENANT_ID))
      .thenReturn(Future.succeededFuture(enabledSetting(true)));

    var instanceTableName = inventoryEventDaoList.getFirst().tableName();
    var marcBibTableName = marcAuditDao.tableName(SourceRecordType.MARC_BIB);
    var marcAuthorityTableName = marcAuditDao.tableName(SourceRecordType.MARC_AUTHORITY);

    var emptySubPartitions = List.of(
      new DatabaseSubPartition(instanceTableName, 0, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 0, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 0, now.getYear(), currentQuarter)
    );

    when(partitionDao.getEmptySubPartitions(TENANT_ID))
      .thenReturn(Future.succeededFuture(emptySubPartitions));
    when(partitionDao.deleteSubPartitions(eq(TENANT_ID), any()))
      .thenReturn(Future.succeededFuture());
    when(partitionDao.createSubPartitions(eq(TENANT_ID), any()))
      .thenReturn(Future.succeededFuture());

    var result = partitionService.cleanUpAndCreateSubPartitions(TENANT_ID);
    result.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(partitionDao, times(1)).getEmptySubPartitions(TENANT_ID);
    verify(partitionDao, times(1)).deleteSubPartitions(TENANT_ID, null);

    // For the current quarter, both inventory tables need partitions (none exist in emptySubPartitions)
    // All 8 partitions for instance and marc_bib tables are created together in one call for INVENTORY_RECORDS_ENABLED setting
    verify(partitionDao, times(1)).createSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(instanceTableName, 0, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(instanceTableName, 1, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(instanceTableName, 2, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(instanceTableName, 3, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(instanceTableName, 4, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(instanceTableName, 5, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(instanceTableName, 6, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(instanceTableName, 7, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 0, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 1, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 2, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 3, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 4, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 5, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 6, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 7, yearForNextQuarter, nextQuarter)
    ));

    // Authority partitions created for the next quarter
    verify(partitionDao, times(1)).createSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(marcAuthorityTableName, 0, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 1, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 2, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 3, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 4, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 5, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 6, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 7, yearForNextQuarter, nextQuarter)
    ));

    verifyNoMoreInteractions(partitionDao);
  }

  @Test
  void testCleanUpAndCreateSubPartitionsSkipsExistingQuarterPartitions() {
    var now = LocalDateTime.now();
    var currentQuarter = YearQuarter.current(now);
    var nextQuarter = YearQuarter.next(now);
    var yearForNextQuarter = currentQuarter.getValue() < nextQuarter.getValue() ? now.getYear() : now.getYear() + 1;

    when(configurationService.getSetting(Setting.INVENTORY_RECORDS_ENABLED, TENANT_ID))
      .thenReturn(Future.succeededFuture(enabledSetting(true)));
    when(configurationService.getSetting(Setting.AUTHORITY_RECORDS_ENABLED, TENANT_ID))
      .thenReturn(Future.succeededFuture(enabledSetting(true)));

    var instanceTableName = inventoryEventDaoList.getFirst().tableName();
    var marcBibTableName = marcAuditDao.tableName(SourceRecordType.MARC_BIB);
    var marcAuthorityTableName = marcAuditDao.tableName(SourceRecordType.MARC_AUTHORITY);

    var emptySubPartitions = List.of(
      new DatabaseSubPartition(instanceTableName, 0, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcBibTableName, 1, yearForNextQuarter, nextQuarter)
    );

    when(partitionDao.getEmptySubPartitions(TENANT_ID))
      .thenReturn(Future.succeededFuture(emptySubPartitions));
    when(partitionDao.deleteSubPartitions(eq(TENANT_ID), any()))
      .thenReturn(Future.succeededFuture());
    when(partitionDao.createSubPartitions(eq(TENANT_ID), any()))
      .thenReturn(Future.succeededFuture());

    var result = partitionService.cleanUpAndCreateSubPartitions(TENANT_ID);
    result.onComplete(ar -> assertTrue(ar.succeeded()));

    verify(partitionDao, times(1)).getEmptySubPartitions(TENANT_ID);
    verify(partitionDao, times(1)).deleteSubPartitions(TENANT_ID, null);

    // For the current quarter, both tables need all partitions created together (inventory setting)
    verify(partitionDao, times(1)).createSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(instanceTableName, 0, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 1, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 2, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 3, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 4, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 5, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 6, now.getYear(), currentQuarter),
      new DatabaseSubPartition(instanceTableName, 7, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 0, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 1, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 2, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 3, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 4, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 5, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 6, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcBibTableName, 7, now.getYear(), currentQuarter)
    ));

    // Authority table for the current quarter
    verify(partitionDao, times(1)).createSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(marcAuthorityTableName, 0, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 1, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 2, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 3, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 4, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 5, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 6, now.getYear(), currentQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 7, now.getYear(), currentQuarter)
    ));

    // Authority table for next quarter (instance already has partition 0, marc_bib has partition 1 for next quarter)
    verify(partitionDao, times(1)).createSubPartitions(TENANT_ID, List.of(
      new DatabaseSubPartition(marcAuthorityTableName, 0, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 1, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 2, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 3, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 4, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 5, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 6, yearForNextQuarter, nextQuarter),
      new DatabaseSubPartition(marcAuthorityTableName, 7, yearForNextQuarter, nextQuarter)
    ));

    verifyNoMoreInteractions(partitionDao);
  }

  private org.folio.rest.jaxrs.model.Setting enabledSetting(boolean enabled) {
    return new org.folio.rest.jaxrs.model.Setting()
      .withKey(SettingKey.ENABLED.getValue())
      .withValue(enabled);
  }
}