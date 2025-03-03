package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.services.configuration.Setting.INVENTORY_RECORDS_PAGE_SIZE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.Vertx;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
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
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.domain.diff.ChangeType;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.util.PostgresClientFactory;
import org.folio.util.inventory.InventoryResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@CopilotGenerated(partiallyGenerated = true)
@ExtendWith(MockitoExtension.class)
public class InventoryAuditApiTest extends ApiTestBase {

  private static final String TENANT_ID = "modaudittest";
  private static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", TENANT_ID);
  private static final Header PERMS_HEADER = new Header("X-Okapi-Permissions", "audit.all");
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers HEADERS = new Headers(TENANT_HEADER, PERMS_HEADER, CONTENT_TYPE_HEADER);
  private static final String INVENTORY_INSTANCE_AUDIT_PATH = "/audit-data/inventory/instance/";
  private static final String INVENTORY_HOLDINGS_AUDIT_PATH = "/audit-data/inventory/holdings/";
  private static final String INVENTORY_ITEM_AUDIT_PATH = "/audit-data/inventory/item/";
  private static final Instant EVENT_DATE = Instant.parse("2025-02-15T13:07:10Z");

  @InjectMocks
  InstanceEventDao instanceEventDao;
  @InjectMocks
  HoldingsEventDao holdingsEventDao;
  @InjectMocks
  ItemEventDao itemEventDao;
  @InjectMocks
  SettingDao settingDao;
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
  @ParameterizedTest
  @MethodSource("provideResourceTypeAndPath")
  void shouldReturnInventoryEventsOnGetByEntityId(InventoryResourceType resourceType, String apiPath) {
    var dao = resourceToDaoMap.get(resourceType);
    var entityId = UUID.randomUUID().toString();
    var changeRecordDto = new ChangeRecordDto();
    changeRecordDto.setFieldChanges(List.of(new FieldChangeDto(ChangeType.MODIFIED, "id", "id", "old", "new")));

    var inventoryAuditEntity = new InventoryAuditEntity(
      UUID.randomUUID(),
      Timestamp.from(EVENT_DATE),
      UUID.fromString(entityId),
      "CREATE",
      UUID.randomUUID(),
      changeRecordDto
    );

    dao.save(inventoryAuditEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    given().headers(HEADERS)
      .get(apiPath + entityId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body(containsString(entityId))
      .body("totalRecords", equalTo(1));
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideResourceTypeAndPath")
  void shouldReturnPaginatedInventoryEvents(InventoryResourceType resourceType, String apiPath) {
    var dao = resourceToDaoMap.get(resourceType);
    // Update the INVENTORY_RECORDS_PAGE_SIZE setting to 3
    var settingEntity = SettingEntity.builder()
      .id(INVENTORY_RECORDS_PAGE_SIZE.getSettingId())
      .key(INVENTORY_RECORDS_PAGE_SIZE.getKey().getValue())
      .value(3)
      .type(SettingValueType.INTEGER)
      .groupId(INVENTORY_RECORDS_PAGE_SIZE.getGroup().getId())
      .updatedDate(LocalDateTime.now())
      .build();

    var entityId = UUID.randomUUID().toString();
    settingDao.update(settingEntity.getId(), settingEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // Create InventoryAuditEntity entities with a day date difference
    for (int i = 0; i < 5; i++) {
      var changeRecordDto = new ChangeRecordDto();
      changeRecordDto.setFieldChanges(List.of(new FieldChangeDto(ChangeType.MODIFIED, "id", "id", "old", "Test Product " + i)));

      var inventoryAuditEntity = new InventoryAuditEntity(
        UUID.randomUUID(),
        Timestamp.from(EVENT_DATE.plusSeconds(86400L * i)), // 1 day difference
        UUID.fromString(entityId),
        "CREATE",
        UUID.randomUUID(),
        changeRecordDto
      );

      dao.save(inventoryAuditEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    }

    // Query the API once
    var firstResponse = given().headers(HEADERS)
      .get(apiPath + entityId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("inventoryAuditItems", hasSize(3))
      .body("totalRecords", equalTo(5))
      .extract().response();

    // Extract the last date from the first response
    var lastDate = firstResponse.jsonPath().getString("inventoryAuditItems[2].eventTs");

    // Query the API again, passing the last date from the previous response
    given().headers(HEADERS)
      .get(apiPath + entityId + "?eventTs=" + lastDate)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("inventoryAuditItems", hasSize(2))
      .body("totalRecords", equalTo(5));
  }

  @ParameterizedTest
  @MethodSource("providePath")
  void shouldReturnEmptyListForNonExistentEntityId(String apiPath) {
    given().headers(HEADERS)
      .get(apiPath + UUID.randomUUID())
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("inventoryAuditItems", hasSize(0))
      .body("totalRecords", equalTo(0));
  }

  @ParameterizedTest
  @MethodSource("providePath")
  void shouldReturn400ForInvalidEntityId(String apiPath) {
    given().headers(HEADERS)
      .get(apiPath + "invalid-id")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
      .body("errors[0].message", containsString("Invalid UUID string"));
  }

  private static Stream<Arguments> provideResourceTypeAndPath() {
    return Stream.of(
      Arguments.of(InventoryResourceType.INSTANCE, INVENTORY_INSTANCE_AUDIT_PATH),
      Arguments.of(InventoryResourceType.HOLDINGS, INVENTORY_HOLDINGS_AUDIT_PATH),
      Arguments.of(InventoryResourceType.ITEM, INVENTORY_ITEM_AUDIT_PATH)
    );
  }

  private static Stream<Arguments> providePath() {
    return Stream.of(
      Arguments.of(INVENTORY_INSTANCE_AUDIT_PATH),
      Arguments.of(INVENTORY_HOLDINGS_AUDIT_PATH),
      Arguments.of(INVENTORY_ITEM_AUDIT_PATH)
    );
  }
}
