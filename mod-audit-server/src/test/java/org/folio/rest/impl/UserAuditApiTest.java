package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
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
import org.folio.domain.diff.FieldChangeDto;
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
    settingDao.update(settingEntity.getId(), settingEntity, TENANT_ID).toCompletionStage().toCompletableFuture().get();

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

  @Test
  void shouldReturn400ForInvalidUserId() {
    given().headers(HEADERS)
      .get(USER_AUDIT_PATH + "invalid-id")
      .then().log().all()
      .statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
      .body("errors[0].message", containsString("Invalid UUID string"));
  }
}
