package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.Vertx;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.CopilotGenerated;
import org.folio.HttpStatus;
import org.folio.dao.configuration.SettingDao;
import org.folio.dao.configuration.SettingEntity;
import org.folio.dao.configuration.SettingValueType;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.dao.inventory.impl.HoldingsEventDao;
import org.folio.dao.inventory.impl.InstanceEventDao;
import org.folio.dao.inventory.impl.InventoryEventDaoImpl;
import org.folio.dao.inventory.impl.ItemEventDao;
import org.folio.dao.management.PartitionDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.dao.marc.impl.MarcAuditDaoImpl;
import org.folio.services.configuration.Setting;
import org.folio.services.configuration.SettingGroup;
import org.folio.services.configuration.SettingKey;
import org.folio.services.management.DatabaseSubPartition;
import org.folio.services.management.YearQuarter;
import org.folio.util.PostgresClientFactory;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.util.marc.SourceRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@CopilotGenerated(partiallyGenerated = true)
@ExtendWith(MockitoExtension.class)
public class AuditDataCleanupApiTest extends ApiTestBase {

  private static final String TENANT_ID = "modaudittest";
  private static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", TENANT_ID);
  private static final Header PERMS_HEADER = new Header("X-Okapi-Permissions", "audit.all");
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers HEADERS = new Headers(TENANT_HEADER, PERMS_HEADER, CONTENT_TYPE_HEADER);

  @InjectMocks
  InstanceEventDao instanceEventDao;
  @InjectMocks
  HoldingsEventDao holdingsEventDao;
  @InjectMocks
  ItemEventDao itemEventDao;
  @InjectMocks
  MarcAuditDaoImpl marcAuditDao;
  @InjectMocks
  SettingDao settingDao;
  @InjectMocks
  PartitionDao partitionDao;
  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  private Map<InventoryResourceType, InventoryEventDaoImpl> resourceToDaoMap;

  @BeforeEach
  void setUp() {
    resourceToDaoMap = new EnumMap<>(InventoryResourceType.class);
    resourceToDaoMap.put(InventoryResourceType.INSTANCE, instanceEventDao);
    resourceToDaoMap.put(InventoryResourceType.HOLDINGS, holdingsEventDao);
    resourceToDaoMap.put(InventoryResourceType.ITEM, itemEventDao);
  }

  @SneakyThrows
  @Test
  void shouldCleanupExpiredRecordsAndManagePartitions() {
    // Prepare retention settings
    setRetentionToOneDay(Setting.INVENTORY_RECORDS_RETENTION_PERIOD, SettingGroup.INVENTORY);
    setRetentionToOneDay(Setting.AUTHORITY_RECORDS_RETENTION_PERIOD, SettingGroup.AUTHORITY);

    // Create 2 audit records for each type
    var entityId = UUID.randomUUID();
    var now = Instant.now();
    var oneDayBefore = Timestamp.from(now.minusSeconds(24 * 60 * 60));
    var oneDayAfter = Timestamp.from(now.plusSeconds(24 * 60 * 60));
    var twoDaysAfter = Timestamp.from(now.plusSeconds(2 * 24 * 60 * 60));
    createAuditRecords(entityId, oneDayBefore, oneDayAfter);

    // Prepare partitions
    var currentDate = LocalDateTime.ofInstant(now,  ZoneId.systemDefault());
    var currentQuarter = YearQuarter.current(currentDate);
    var previousQuarter = currentQuarter.getValue() == 1 ? YearQuarter.fromValue(4) : YearQuarter.fromValue(currentQuarter.getValue() - 1);
    var nextQuarter = YearQuarter.next(currentDate);
    var yearForPreviousQuarter = currentQuarter.getValue() > previousQuarter.getValue() ? currentDate.getYear() : currentDate.getYear() - 1;
    var yearForNextQuarter = currentQuarter.getValue() < nextQuarter.getValue() ? currentDate.getYear() : currentDate.getYear() + 1;

    var tableNames = new LinkedList<>(resourceToDaoMap.values().stream().map(InventoryEventDaoImpl::tableName).toList());
    tableNames.add(marcAuditDao.tableName(SourceRecordType.MARC_BIB));
    tableNames.add(marcAuditDao.tableName(SourceRecordType.MARC_AUTHORITY));

    var emptySubPartitions = tableNames.stream()
      .map(tableName -> new DatabaseSubPartition(tableName, 0, yearForPreviousQuarter, previousQuarter))
      .toList();

    partitionDao.createSubPartitions(TENANT_ID, emptySubPartitions).toCompletionStage().toCompletableFuture().get();

    // Trigger the cleanup
    given().headers(HEADERS)
      .post("/audit-data/cleanup/timer")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    // Verify that one record for each type is deleted and one remains
    verifyRemainingRecords(entityId, twoDaysAfter);

    // Verify partitions
    verifyPartitions(tableNames, yearForPreviousQuarter, previousQuarter, yearForNextQuarter, nextQuarter);
  }

  @SneakyThrows
  private void setRetentionToOneDay(Setting setting, SettingGroup settingGroup) {
    var settingEntity = SettingEntity.builder()
      .id(setting.getSettingId())
      .key(SettingKey.RETENTION_PERIOD.getValue())
      .value(1)
      .type(SettingValueType.INTEGER)
      .groupId(settingGroup.getId())
      .build();
    settingDao.update(settingEntity.getId(), settingEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();
  }

  @SneakyThrows
  private void createAuditRecords(UUID entityId, Timestamp oneDayBefore, Timestamp oneDayAfter) {
    // Create records for each entity type
    createInventoryAuditEntities(instanceEventDao, entityId, oneDayBefore, oneDayAfter);
    createInventoryAuditEntities(holdingsEventDao, entityId, oneDayBefore, oneDayAfter);
    createInventoryAuditEntities(itemEventDao, entityId, oneDayBefore, oneDayAfter);
    createMarcAuditEntities(SourceRecordType.MARC_BIB, entityId, oneDayBefore, oneDayAfter);
    createMarcAuditEntities(SourceRecordType.MARC_AUTHORITY, entityId, oneDayBefore, oneDayAfter);
  }

  @SneakyThrows
  private void createInventoryAuditEntities(InventoryEventDaoImpl dao, UUID entityId, Timestamp oneDayBefore, Timestamp oneDayAfter) {
    var beforeEntity = new InventoryAuditEntity(UUID.randomUUID(), oneDayBefore, entityId, "CREATE", UUID.randomUUID(), null);
    var afterEntity = new InventoryAuditEntity(UUID.randomUUID(), oneDayAfter, entityId, "CREATE", UUID.randomUUID(), null);

    dao.save(beforeEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    dao.save(afterEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();
  }

  @SneakyThrows
  private void createMarcAuditEntities(SourceRecordType recordType, UUID entityId, Timestamp oneDayBefore, Timestamp oneDayAfter) {
    var beforeEntity = new MarcAuditEntity(UUID.randomUUID().toString(), oneDayBefore.toLocalDateTime(), entityId.toString(), "origin", "CREATE", UUID.randomUUID().toString(), null);
    var afterEntity = new MarcAuditEntity(UUID.randomUUID().toString(), oneDayAfter.toLocalDateTime(), entityId.toString(), "origin", "CREATE", UUID.randomUUID().toString(), null);

    marcAuditDao.save(beforeEntity, recordType, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    marcAuditDao.save(afterEntity, recordType, TENANT_ID).toCompletionStage().toCompletableFuture().get();
  }

  @SneakyThrows
  private void verifyRemainingRecords(UUID entityId, Timestamp twoDaysAfter) {
    verifyRemainingInventoryRecords(instanceEventDao, entityId, twoDaysAfter);
    verifyRemainingInventoryRecords(holdingsEventDao, entityId, twoDaysAfter);
    verifyRemainingInventoryRecords(itemEventDao, entityId, twoDaysAfter);
    verifyRemainingMarcRecords(SourceRecordType.MARC_BIB, entityId, twoDaysAfter);
    verifyRemainingMarcRecords(SourceRecordType.MARC_AUTHORITY, entityId, twoDaysAfter);
  }

  @SneakyThrows
  private void verifyRemainingInventoryRecords(InventoryEventDaoImpl dao, UUID entityId, Timestamp twoDaysAfter) {
    var remainingRecords = dao.get(entityId, twoDaysAfter, 10, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    assertThat(remainingRecords).hasSize(1);
  }

  @SneakyThrows
  private void verifyRemainingMarcRecords(SourceRecordType recordType, UUID entityId, Timestamp twoDaysAfter) {
    var remainingRecords = marcAuditDao.get(entityId, recordType, TENANT_ID, twoDaysAfter.toLocalDateTime(), 10).toCompletionStage().toCompletableFuture().get();
    assertThat(remainingRecords).hasSize(1);
  }

  @SneakyThrows
  private void verifyPartitions(List<String> tableNames, int yearForPreviousQuarter, YearQuarter previousQuarter, int yearForNextQuarter, YearQuarter nextQuarter) {
    var remainingPartitions = partitionDao.getEmptySubPartitions(TENANT_ID).toCompletionStage().toCompletableFuture().get();

    for (String tableName : tableNames) {
      // Verify that partitions for the previous year/quarter do not exist
      assertThat(remainingPartitions).doesNotContain(
        new DatabaseSubPartition(tableName, 0, yearForPreviousQuarter, previousQuarter)
      );

      // Verify that partitions for the next year/quarter exist
      for (int i = 0; i < 8; i++) {
        assertThat(remainingPartitions).contains(
          new DatabaseSubPartition(tableName, i, yearForNextQuarter, nextQuarter)
        );
      }
    }
  }
}