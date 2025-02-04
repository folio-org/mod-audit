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
import java.util.UUID;
import org.folio.CopilotGenerated;
import org.folio.HttpStatus;
import org.folio.dao.configuration.SettingDao;
import org.folio.dao.configuration.SettingEntity;
import org.folio.dao.configuration.SettingValueType;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.dao.inventory.impl.InstanceEventDao;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

@CopilotGenerated(partiallyGenerated = true)
public class InventoryAuditApiTest extends ApiTestBase {

  private static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", "modaudittest");
  private static final Header PERMS_HEADER = new Header("X-Okapi-Permissions", "audit.all");
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers HEADERS = new Headers(TENANT_HEADER, PERMS_HEADER, CONTENT_TYPE_HEADER);
  private static final String INVENTORY_INSTANCE_AUDIT_PATH = "/audit-data/inventory/instance/";
  private static final String TENANT_ID = "modaudittest";
  private static final String ENTITY_ID = UUID.randomUUID().toString();

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  InstanceEventDao instanceEventDao;
  @InjectMocks
  SettingDao settingDao;

  @BeforeEach
  public void setUp() {
    instanceEventDao = new InstanceEventDao(postgresClientFactory);
    settingDao = new SettingDao(postgresClientFactory);
  }

  @Test
  void shouldReturnInventoryEventsOnGetByEntityId() {
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

    instanceEventDao.save(inventoryAuditEntity, TENANT_ID);

    given().headers(HEADERS)
      .get(INVENTORY_INSTANCE_AUDIT_PATH + ENTITY_ID)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body(containsString(ENTITY_ID));
  }

  @Test
  void shouldReturnPaginatedInventoryEvents() {
    // Update the INVENTORY_RECORDS_PAGE_SIZE setting to 3
    var settingEntity = SettingEntity.builder()
      .id(INVENTORY_RECORDS_PAGE_SIZE.getSettingId())
      .key(INVENTORY_RECORDS_PAGE_SIZE.getKey())
      .value(3)
      .type(SettingValueType.INTEGER)
      .groupId(INVENTORY_RECORDS_PAGE_SIZE.getGroup().getId())
      .updatedDate(LocalDateTime.now())
      .build();
    settingDao.update(settingEntity.getId(), settingEntity, TENANT_ID).result();

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

      instanceEventDao.save(inventoryAuditEntity, TENANT_ID).result();
    }

    // Query the API once
    var firstResponse = given().headers(HEADERS)
      .get(INVENTORY_INSTANCE_AUDIT_PATH + ENTITY_ID)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("inventoryAuditItems", hasSize(3))
      .extract().response();

    // Extract the last date from the first response
    var lastDate = firstResponse.jsonPath().getString("inventoryAuditItems[2].eventTs");

    // Query the API again, passing the last date from the previous response
    given().headers(HEADERS)
      .get(INVENTORY_INSTANCE_AUDIT_PATH + ENTITY_ID + "?eventTs=" + lastDate)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("inventoryAuditItems", hasSize(2));
  }

  @Test
  void shouldReturnEmptyListForInvalidEntityId() {
    given().headers(HEADERS)
      .get(INVENTORY_INSTANCE_AUDIT_PATH + UUID.randomUUID().toString())
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("inventoryAuditItems", hasSize(0));
  }
}