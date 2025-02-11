package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.services.configuration.Setting.INVENTORY_RECORDS_PAGE_SIZE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
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
  private static final String ENTITY_ID = UUID.randomUUID().toString();
  private static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", TENANT_ID);
  private static final Header PERMS_HEADER = new Header("X-Okapi-Permissions", "audit.all");
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers HEADERS = new Headers(TENANT_HEADER, PERMS_HEADER, CONTENT_TYPE_HEADER);
  private static final String INVENTORY_INSTANCE_AUDIT_PATH = "/audit-data/inventory/instance/";
  private static final String INVENTORY_HOLDINGS_AUDIT_PATH = "/audit-data/inventory/holdings/";

  @InjectMocks
  InstanceEventDao instanceEventDao;
  @InjectMocks
  HoldingsEventDao holdingsEventDao;
  @InjectMocks
  SettingDao settingDao;
  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  private Map<InventoryResourceType, InventoryEventDaoImpl> resourceToDaoMap;

  @BeforeEach
  public void setUp() {
    resourceToDaoMap = new EnumMap<>(InventoryResourceType.class);
    resourceToDaoMap.put(InventoryResourceType.INSTANCE, instanceEventDao);
    resourceToDaoMap.put(InventoryResourceType.HOLDINGS, holdingsEventDao);
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideResourceTypeAndPath")
  void shouldReturnInventoryEventsOnGetByEntityId(InventoryResourceType resourceType, String apiPath) {
    var dao = resourceToDaoMap.get(resourceType);
    var jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product");

    var inventoryAuditEntity = new InventoryAuditEntity(
      UUID.randomUUID(),
      Timestamp.from(Instant.now()),
      UUID.fromString(ENTITY_ID),
      "CREATE",
      UUID.randomUUID(),
      jsonObject.getMap()
    );

    dao.save(inventoryAuditEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    given().headers(HEADERS)
      .get(apiPath + ENTITY_ID)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body(containsString(ENTITY_ID));
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideResourceTypeAndPath")
  void shouldReturnPaginatedInventoryEvents(InventoryResourceType resourceType, String apiPath) {
    var dao = resourceToDaoMap.get(resourceType);
    // Update the INVENTORY_RECORDS_PAGE_SIZE setting to 3
    var settingEntity = SettingEntity.builder()
      .id(INVENTORY_RECORDS_PAGE_SIZE.getSettingId())
      .key(INVENTORY_RECORDS_PAGE_SIZE.getKey())
      .value(3)
      .type(SettingValueType.INTEGER)
      .groupId(INVENTORY_RECORDS_PAGE_SIZE.getGroup().getId())
      .updatedDate(LocalDateTime.now())
      .build();
    settingDao.update(settingEntity.getId(), settingEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // Create InventoryAuditEntity entities with a year date difference
    for (int i = 0; i < 5; i++) {
      var jsonObject = new JsonObject();
      jsonObject.put("name", "Test Product " + i);

      var inventoryAuditEntity = new InventoryAuditEntity(
        UUID.randomUUID(),
        Timestamp.from(Instant.now().plusSeconds(2628000L * i)), // 1 month difference
        UUID.fromString(ENTITY_ID),
        "CREATE",
        UUID.randomUUID(),
        jsonObject.getMap()
      );

      dao.save(inventoryAuditEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    }

    // Query the API once
    var firstResponse = given().headers(HEADERS)
      .get(apiPath + ENTITY_ID)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("inventoryAuditItems", hasSize(3))
      .extract().response();

    // Extract the last date from the first response
    var lastDate = firstResponse.jsonPath().getString("inventoryAuditItems[2].eventTs");

    // Query the API again, passing the last date from the previous response
    given().headers(HEADERS)
      .get(apiPath + ENTITY_ID + "?eventTs=" + lastDate)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("inventoryAuditItems", hasSize(2));
  }

  @ParameterizedTest
  @MethodSource("providePath")
  void shouldReturnEmptyListForInvalidEntityId(String apiPath) {
    given().headers(HEADERS)
      .get(apiPath + UUID.randomUUID())
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("inventoryAuditItems", hasSize(0));
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
      Arguments.of(InventoryResourceType.HOLDINGS, INVENTORY_HOLDINGS_AUDIT_PATH)
    );
  }

  private static Stream<Arguments> providePath() {
    return Stream.of(
      Arguments.of(INVENTORY_INSTANCE_AUDIT_PATH),
      Arguments.of(INVENTORY_HOLDINGS_AUDIT_PATH)
    );
  }
}