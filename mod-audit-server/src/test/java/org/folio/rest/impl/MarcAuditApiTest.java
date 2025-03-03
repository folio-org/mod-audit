package org.folio.rest.impl;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.folio.HttpStatus;
import org.folio.dao.configuration.SettingDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.dao.marc.impl.MarcAuditDaoImpl;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(MockitoExtension.class)
public class MarcAuditApiTest extends ApiTestBase {

  private static final String TENANT_ID = "modaudittest";
  private static final String ENTITY_ID = UUID.randomUUID().toString();
  private static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", TENANT_ID);
  private static final Header PERMS_HEADER = new Header("X-Okapi-Permissions", "audit.all");
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers HEADERS = new Headers(TENANT_HEADER, PERMS_HEADER, CONTENT_TYPE_HEADER);
  private static final String MARC_BIB_PATH = "/audit-data/marc/bib/";
  private static final String MARC_AUTHORITY_PATH = "/audit-data/marc/authority/";
  private static final String DIFF = """
    {
        "fieldChanges": [{"fullPath": "019", "newValue": "01016cam a2200325Mu 4500", "oldValue": "00962cam a2200301Mu 4500", "fieldName": "LDR", "changeType": "MODIFIED"}]
    }
    """;
  @InjectMocks
  MarcAuditDaoImpl marcAuditDao;
  @InjectMocks
  SettingDao settingDao;
  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideSourceTypeAndPath")
  void shouldReturnMarcEventsOnGetByEntityId(SourceRecordType recordType, String apiPath) {
    var entity = new MarcAuditEntity(
      UUID.randomUUID().toString(),
      Timestamp.from(Instant.now()).toLocalDateTime(),
      ENTITY_ID,
      "origin",
      "CREATE",
      UUID.randomUUID().toString(),
      new JsonObject(DIFF).mapTo(ChangeRecordDto.class)
    );

    marcAuditDao.save(entity, recordType, TENANT_ID).toCompletionStage().toCompletableFuture().get();

    given().headers(HEADERS)
      .get(apiPath + ENTITY_ID)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body(containsString(ENTITY_ID))
      .body("totalRecords", equalTo(1));
  }

  @ParameterizedTest
  @MethodSource("providePath")
  void shouldReturnEmptyListForNonExistentEntityId(String apiPath) {
    given().headers(HEADERS)
      .get(apiPath + UUID.randomUUID())
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .body("marcAuditItems", hasSize(0))
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

  private static Stream<Arguments> provideSourceTypeAndPath() {
    return Stream.of(
      Arguments.of(SourceRecordType.MARC_BIB, MARC_BIB_PATH),
      Arguments.of(SourceRecordType.MARC_AUTHORITY, MARC_AUTHORITY_PATH)
    );
  }

  private static Stream<Arguments> providePath() {
    return Stream.of(
      Arguments.of(MARC_BIB_PATH),
      Arguments.of(MARC_AUTHORITY_PATH)
    );
  }
}
