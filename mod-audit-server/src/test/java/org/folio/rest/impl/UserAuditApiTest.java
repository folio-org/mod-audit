package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.services.configuration.Setting.USER_RECORDS_PAGE_SIZE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.Vertx;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.CopilotGenerated;
import org.folio.HttpStatus;
import org.folio.dao.configuration.SettingDao;
import org.folio.dao.configuration.SettingEntity;
import org.folio.dao.configuration.SettingValueType;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.impl.UserEventDaoImpl;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.domain.diff.ChangeType;
import org.folio.domain.diff.CollectionChangeDto;
import org.folio.domain.diff.CollectionItemChangeDto;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@CopilotGenerated(partiallyGenerated = true)
@ExtendWith(MockitoExtension.class)
public class UserAuditApiTest extends ApiTestBase {

  private static final String TENANT_ID = "modaudittest";
  private static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", TENANT_ID);
  private static final Header PERMS_HEADER = new Header("X-Okapi-Permissions", "audit.all");
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers HEADERS = new Headers(TENANT_HEADER, PERMS_HEADER, CONTENT_TYPE_HEADER);
  private static final Header CONFIG_PERMS_HEADER = new Header(XOkapiHeaders.PERMISSIONS, """
    ["audit.config.groups.settings.audit.user.anonymize.item.put",
    "audit.config.groups.settings.audit.user.excluded.fields.item.put"]""");
  private static final Header USER_HEADER = new Header(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());
  private static final Headers CONFIG_HEADERS =
    new Headers(TENANT_HEADER, CONFIG_PERMS_HEADER, USER_HEADER, CONTENT_TYPE_HEADER);
  private static final String USER_AUDIT_PATH = "/audit-data/user/";

  @InjectMocks
  UserEventDaoImpl userEventDao;
  @InjectMocks
  SettingDao settingDao;
  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @SneakyThrows
  @Test
  void shouldReturnUserEventsOnGetByUserId() {
    var userId = UUID.randomUUID().toString();
    var changeRecordDto = new ChangeRecordDto();
    changeRecordDto.setFieldChanges(List.of(new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new")));

    var userAuditEntity = new UserAuditEntity(
      UUID.randomUUID(),
      Timestamp.from(Instant.now()),
      UUID.fromString(userId),
      "CREATE",
      UUID.randomUUID(),
      changeRecordDto
    );

    userEventDao.save(userAuditEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body(containsString(userId))
      .body("totalRecords", equalTo(1));
  }

  @SneakyThrows
  @Test
  void shouldReturnPaginatedUserEvents() {
    var settingEntity = SettingEntity.builder()
      .id(USER_RECORDS_PAGE_SIZE.getSettingId())
      .key(USER_RECORDS_PAGE_SIZE.getKey().getValue())
      .value(3)
      .type(SettingValueType.INTEGER)
      .groupId(USER_RECORDS_PAGE_SIZE.getGroup().getId())
      .updatedDate(LocalDateTime.now())
      .build();

    var userId = UUID.randomUUID().toString();
    postgresClientFactory.createInstance(TENANT_ID)
      .withTrans(conn -> settingDao.update(settingEntity.getId(), settingEntity, conn, TENANT_ID))
      .toCompletionStage().toCompletableFuture().get();

    for (int i = 0; i < 5; i++) {
      var changeRecordDto = new ChangeRecordDto();
      changeRecordDto.setFieldChanges(List.of(new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "User " + i)));

      var userAuditEntity = new UserAuditEntity(
        UUID.randomUUID(),
        Timestamp.from(Instant.now().plusSeconds(86400L * i)),
        UUID.fromString(userId),
        "CREATE",
        UUID.randomUUID(),
        changeRecordDto
      );

      userEventDao.save(userAuditEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    }

    var firstResponse = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("userAuditItems", hasSize(3))
      .body("totalRecords", equalTo(5))
      .extract().response();

    var lastDate = firstResponse.jsonPath().getString("userAuditItems[2].eventTs");

    given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId + "?eventTs=" + lastDate)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("userAuditItems", hasSize(2))
      .body("totalRecords", equalTo(5));
  }

  @Test
  void shouldReturnEmptyListForNonExistentUserId() {
    given().headers(HEADERS)
      .get(USER_AUDIT_PATH + UUID.randomUUID())
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("userAuditItems", hasSize(0))
      .body("totalRecords", equalTo(0));
  }

  @SneakyThrows
  @Test
  void shouldAnonymizeExistingRecordsWhenAnonymizeSettingEnabled() {
    var userId = UUID.randomUUID();
    var performedBy = UUID.randomUUID();

    // Record A: UPDATE with username + metadata.updatedByUserId changes
    var diffA = new ChangeRecordDto(List.of(
      new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new"),
      new FieldChangeDto(ChangeType.MODIFIED, "updatedByUserId", "metadata.updatedByUserId",
        UUID.randomUUID().toString(), UUID.randomUUID().toString())
    ), null);
    var recordA = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(300)),
      userId, "UPDATED", performedBy, diffA);

    // Record B: UPDATE with only metadata.createdByUserId change
    var diffB = new ChangeRecordDto(List.of(
      new FieldChangeDto(ChangeType.MODIFIED, "createdByUserId", "metadata.createdByUserId",
        UUID.randomUUID().toString(), UUID.randomUUID().toString())
    ), null);
    var recordB = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(200)),
      userId, "UPDATED", performedBy, diffB);

    // Record C: CREATED with no diff
    var recordC = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(100)),
      userId, "CREATED", performedBy, null);

    // Record D: UPDATE with only anonymized fieldChanges BUT non-empty collectionChanges — should survive
    var diffD = new ChangeRecordDto(
      List.of(new FieldChangeDto(ChangeType.MODIFIED, "updatedByUserId", "metadata.updatedByUserId",
        UUID.randomUUID().toString(), UUID.randomUUID().toString())),
      List.of(new CollectionChangeDto("departments", "departments", List.of(
        CollectionItemChangeDto.added("dept-new")))));
    var recordD = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(50)),
      userId, "UPDATED", performedBy, diffD);

    userEventDao.save(recordA, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    userEventDao.save(recordB, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    userEventDao.save(recordC, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    userEventDao.save(recordD, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // Enable anonymization via config API
    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"anonymize","value":true,"groupId":"audit.user","type":"BOOLEAN"}""")
      .put("/audit/config/groups/audit.user/settings/anonymize")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    // Fetch user audit records
    var response = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().response();

    // Record B deleted (empty UPDATE after anonymization), A/C/D remain
    assertThat(response.jsonPath().getInt("totalRecords")).isEqualTo(3);
    var items = response.jsonPath().getList("userAuditItems");
    assertThat(items).hasSize(3);

    // Record D (most recent, UPDATED): performedBy null, fieldChanges stripped, collectionChanges preserved
    assertThat(response.jsonPath().getString("userAuditItems[0].performedBy")).isNull();
    assertThat(response.jsonPath().getList("userAuditItems[0].diff.fieldChanges")).isEmpty();
    assertThat(response.jsonPath().getList("userAuditItems[0].diff.collectionChanges")).hasSize(1);

    // Record C (CREATED): performedBy null, diff null
    assertThat(response.jsonPath().getString("userAuditItems[1].performedBy")).isNull();
    assertThat((Object) response.jsonPath().get("userAuditItems[1].diff")).isNull();

    // Record A (oldest UPDATED): performedBy null, only username field change remains
    assertThat(response.jsonPath().getString("userAuditItems[2].performedBy")).isNull();
    var fieldChanges = response.jsonPath().getList("userAuditItems[2].diff.fieldChanges");
    assertThat(fieldChanges).hasSize(1);
    assertThat(response.jsonPath().getString("userAuditItems[2].diff.fieldChanges[0].fullPath")).isEqualTo("username");
  }

  @SneakyThrows
  @Test
  void shouldExcludeFieldsFromExistingRecordsWhenExcludedFieldsSettingUpdated() {
    var userId = UUID.randomUUID();
    var performedBy = UUID.randomUUID();

    // Record A: UPDATE with username + personal.email changes
    var diffA = new ChangeRecordDto(List.of(
      new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new"),
      new FieldChangeDto(ChangeType.MODIFIED, "email", "personal.email", "old@test.com", "new@test.com")
    ), null);
    var recordA = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(300)),
      userId, "UPDATED", performedBy, diffA);

    // Record B: UPDATE with only personal.email change — should be deleted after exclusion
    var diffB = new ChangeRecordDto(List.of(
      new FieldChangeDto(ChangeType.MODIFIED, "email", "personal.email", "a@test.com", "b@test.com")
    ), null);
    var recordB = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(200)),
      userId, "UPDATED", performedBy, diffB);

    // Record C: CREATED with no diff — should be unaffected
    var recordC = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(100)),
      userId, "CREATED", performedBy, null);

    userEventDao.save(recordA, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    userEventDao.save(recordB, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    userEventDao.save(recordC, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // Update excluded fields via config API
    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"excluded.fields","value":"[\\"personal.email\\"]","groupId":"audit.user","type":"STRING"}""")
      .put("/audit/config/groups/audit.user/settings/excluded.fields")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    // Fetch user audit records
    var response = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().response();

    // Record B deleted (empty UPDATE after exclusion), A/C remain
    assertThat(response.jsonPath().getInt("totalRecords")).isEqualTo(2);
    var items = response.jsonPath().getList("userAuditItems");
    assertThat(items).hasSize(2);

    // Record C (most recent, CREATED): unaffected
    assertThat(response.jsonPath().getString("userAuditItems[0].action")).isEqualTo("CREATED");
    assertThat((Object) response.jsonPath().get("userAuditItems[0].diff")).isNull();

    // Record A (oldest UPDATED): only username field change remains, personal.email excluded
    assertThat(response.jsonPath().getString("userAuditItems[1].action")).isEqualTo("UPDATED");
    var fieldChanges = response.jsonPath().getList("userAuditItems[1].diff.fieldChanges");
    assertThat(fieldChanges).hasSize(1);
    assertThat(response.jsonPath().getString("userAuditItems[1].diff.fieldChanges[0].fullPath")).isEqualTo("username");
  }

  @SneakyThrows
  @Test
  void shouldExcludeCollectionChangesFromExistingRecords() {
    var userId = UUID.randomUUID();
    var performedBy = UUID.randomUUID();

    // Record A: UPDATE with field change + collection change (departments)
    var diffA = new ChangeRecordDto(
      List.of(new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new")),
      List.of(new CollectionChangeDto("departments", "departments",
        List.of(CollectionItemChangeDto.added("dept1"))))
    );
    var recordA = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(300)),
      userId, "UPDATED", performedBy, diffA);

    // Record B: UPDATE with only collection change — should be deleted after exclusion
    var diffB = new ChangeRecordDto(null,
      List.of(new CollectionChangeDto("departments", "departments",
        List.of(CollectionItemChangeDto.added("dept2"))))
    );
    var recordB = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now().minusSeconds(200)),
      userId, "UPDATED", performedBy, diffB);

    userEventDao.save(recordA, TENANT_ID).toCompletionStage().toCompletableFuture().get();
    userEventDao.save(recordB, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"excluded.fields","value":"[\\"departments\\"]","groupId":"audit.user","type":"STRING"}""")
      .put("/audit/config/groups/audit.user/settings/excluded.fields")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    var response = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().response();

    // Record B deleted (empty UPDATE), Record A survives with username field only
    assertThat(response.jsonPath().getInt("totalRecords")).isEqualTo(1);
    assertThat(response.jsonPath().getString("userAuditItems[0].action")).isEqualTo("UPDATED");
    assertThat(response.jsonPath().getList("userAuditItems[0].diff.fieldChanges")).hasSize(1);
    assertThat(response.jsonPath().getString("userAuditItems[0].diff.fieldChanges[0].fullPath")).isEqualTo("username");
    assertThat(response.jsonPath().getList("userAuditItems[0].diff.collectionChanges")).isEmpty();
  }

  @SneakyThrows
  @Test
  void shouldExcludeFieldsByPrefixMatching() {
    var userId = UUID.randomUUID();
    var performedBy = UUID.randomUUID();

    // Record with customFields.myField and customFields.otherField + username
    var diff = new ChangeRecordDto(List.of(
      new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new"),
      new FieldChangeDto(ChangeType.MODIFIED, "myField", "customFields.myField", "old", "new"),
      new FieldChangeDto(ChangeType.MODIFIED, "otherField", "customFields.otherField", "old", "new")
    ), null);
    var entity = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()),
      userId, "UPDATED", performedBy, diff);

    userEventDao.save(entity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // Exclude "customFields" — should match customFields.myField and customFields.otherField by prefix
    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"excluded.fields","value":"[\\"customFields\\"]","groupId":"audit.user","type":"STRING"}""")
      .put("/audit/config/groups/audit.user/settings/excluded.fields")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    var response = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().response();

    assertThat(response.jsonPath().getInt("totalRecords")).isEqualTo(1);
    var fieldChanges = response.jsonPath().getList("userAuditItems[0].diff.fieldChanges");
    assertThat(fieldChanges).hasSize(1);
    assertThat(response.jsonPath().getString("userAuditItems[0].diff.fieldChanges[0].fullPath")).isEqualTo("username");
  }

  @SneakyThrows
  @Test
  void shouldExcludeMixedFieldAndCollectionChanges() {
    var userId = UUID.randomUUID();
    var performedBy = UUID.randomUUID();

    // Record with field changes (username, personal.email) + collection change (departments)
    var diff = new ChangeRecordDto(
      List.of(
        new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new"),
        new FieldChangeDto(ChangeType.MODIFIED, "email", "personal.email", "old@t.com", "new@t.com")
      ),
      List.of(new CollectionChangeDto("departments", "departments",
        List.of(CollectionItemChangeDto.added("dept1"))))
    );
    var entity = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()),
      userId, "UPDATED", performedBy, diff);

    userEventDao.save(entity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // Exclude personal.email and departments — username should survive
    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"excluded.fields","value":"[\\"personal.email\\",\\"departments\\"]","groupId":"audit.user","type":"STRING"}""")
      .put("/audit/config/groups/audit.user/settings/excluded.fields")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    var response = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().response();

    assertThat(response.jsonPath().getInt("totalRecords")).isEqualTo(1);
    assertThat(response.jsonPath().getList("userAuditItems[0].diff.fieldChanges")).hasSize(1);
    assertThat(response.jsonPath().getString("userAuditItems[0].diff.fieldChanges[0].fullPath")).isEqualTo("username");
    assertThat(response.jsonPath().getList("userAuditItems[0].diff.collectionChanges")).isEmpty();
  }

  @SneakyThrows
  @Test
  void shouldNotAffectRecordsWhenExcludedFieldsNotPresent() {
    var userId = UUID.randomUUID();
    var performedBy = UUID.randomUUID();

    var diff = new ChangeRecordDto(List.of(
      new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new"),
      new FieldChangeDto(ChangeType.MODIFIED, "barcode", "barcode", "111", "222")
    ), null);
    var entity = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()),
      userId, "UPDATED", performedBy, diff);

    userEventDao.save(entity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // Exclude "personal.email" which is not present in the record
    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"excluded.fields","value":"[\\"personal.email\\"]","groupId":"audit.user","type":"STRING"}""")
      .put("/audit/config/groups/audit.user/settings/excluded.fields")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    var response = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().response();

    // Record unchanged — both field changes survive
    assertThat(response.jsonPath().getInt("totalRecords")).isEqualTo(1);
    assertThat(response.jsonPath().getList("userAuditItems[0].diff.fieldChanges")).hasSize(2);
  }

  @SneakyThrows
  @Test
  void shouldApplyBothExclusionAndAnonymizationOnExistingRecords() {
    var userId = UUID.randomUUID();
    var performedBy = UUID.randomUUID();

    // Record with username + personal.email + metadata.updatedByUserId
    var diff = new ChangeRecordDto(List.of(
      new FieldChangeDto(ChangeType.MODIFIED, "username", "username", "old", "new"),
      new FieldChangeDto(ChangeType.MODIFIED, "email", "personal.email", "old@t.com", "new@t.com"),
      new FieldChangeDto(ChangeType.MODIFIED, "updatedByUserId", "metadata.updatedByUserId",
        UUID.randomUUID().toString(), UUID.randomUUID().toString())
    ), null);
    var entity = new UserAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()),
      userId, "UPDATED", performedBy, diff);

    userEventDao.save(entity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    // First exclude personal.email
    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"excluded.fields","value":"[\\"personal.email\\"]","groupId":"audit.user","type":"STRING"}""")
      .put("/audit/config/groups/audit.user/settings/excluded.fields")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    // Then enable anonymization
    given().headers(CONFIG_HEADERS)
      .body("""
        {"key":"anonymize","value":true,"groupId":"audit.user","type":"BOOLEAN"}""")
      .put("/audit/config/groups/audit.user/settings/anonymize")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    var response = given().headers(HEADERS)
      .get(USER_AUDIT_PATH + userId)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().response();

    // personal.email excluded, metadata.updatedByUserId anonymized, performedBy nullified
    assertThat(response.jsonPath().getInt("totalRecords")).isEqualTo(1);
    assertThat(response.jsonPath().getString("userAuditItems[0].performedBy")).isNull();
    var fieldChanges = response.jsonPath().getList("userAuditItems[0].diff.fieldChanges");
    assertThat(fieldChanges).hasSize(1);
    assertThat(response.jsonPath().getString("userAuditItems[0].diff.fieldChanges[0].fullPath")).isEqualTo("username");
  }

  @Test
  void shouldReturn400ForInvalidUserId() {
    given().headers(HEADERS)
      .get(USER_AUDIT_PATH + "invalid-id")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
      .body("errors[0].message", containsString("Invalid UUID string"));
  }
}
