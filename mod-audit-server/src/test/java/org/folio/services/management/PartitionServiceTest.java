package org.folio.services.management;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    var instanceTableName = inventoryEventDaoList.get(0).tableName();
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
  }

  private org.folio.rest.jaxrs.model.Setting enabledSetting(boolean enabled) {
    return new org.folio.rest.jaxrs.model.Setting()
      .withKey(SettingKey.ENABLED.getValue())
      .withValue(enabled);
  }
}