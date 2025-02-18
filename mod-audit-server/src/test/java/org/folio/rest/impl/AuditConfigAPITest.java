package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dao.configuration.SettingValueType.BOOLEAN;
import static org.folio.dao.configuration.SettingValueType.INTEGER;
import static org.folio.services.configuration.SettingGroup.AUTHORITY;
import static org.folio.services.configuration.SettingGroup.INVENTORY;
import static org.folio.services.configuration.SettingKey.ENABLED;
import static org.folio.services.configuration.SettingKey.RECORDS_PAGE_SIZE;
import static org.folio.services.configuration.SettingKey.RETENTION_PERIOD;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.HttpStatus;
import org.folio.dao.configuration.SettingValueType;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.SettingCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AuditConfigAPITest extends ApiTestBase {

  private static final Header TENANT_HEADER = new Header(XOkapiHeaders.TENANT, "modaudittest");
  private static final Header INVENTORY_PERMS_HEADER = new Header(XOkapiHeaders.PERMISSIONS, """
    ["audit.config.groups.settings.collection.get",
    "audit.config.groups.settings.audit.inventory.enabled.item.put",
    "audit.config.groups.settings.audit.inventory.records.page.size.item.put",
    "audit.config.groups.settings.audit.inventory.records.retention.period.item.put",
    "audit.config.groups.settings.audit.inventory.collection.get"]""");
  private static final Header AUTHORITY_PERMS_HEADER = new Header(XOkapiHeaders.PERMISSIONS, """
    ["audit.config.groups.settings.collection.get",
    "audit.config.groups.settings.audit.authority.enabled.item.put",
    "audit.config.groups.settings.audit.authority.records.page.size.item.put",
    "audit.config.groups.settings.audit.authority.records.retention.period.item.put",
    "audit.config.groups.settings.audit.authority.collection.get"]""");
  private static final Header USER_HEADER = new Header(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers INVENTORY_HEADERS =
    new Headers(TENANT_HEADER, INVENTORY_PERMS_HEADER, USER_HEADER, CONTENT_TYPE_HEADER);
  private static final Headers AUTHORITY_HEADERS =
    new Headers(TENANT_HEADER, AUTHORITY_PERMS_HEADER, USER_HEADER, CONTENT_TYPE_HEADER);
  private static final String AUDIT_CONFIG_GROUPS_PATH = "/audit/config/groups";
  private static final String AUDIT_CONFIG_SETTINGS_PATH = "/audit/config/groups/%s/settings";
  private static final String AUDIT_CONFIG_SETTING_ENTRY_PATH = "/audit/config/groups/%s/settings/%s";

  @Test
  void shouldReturnSettingGroupCollection() {
    given().headers(INVENTORY_HEADERS).get(AUDIT_CONFIG_GROUPS_PATH)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("totalRecords", equalTo(2))
      .body("settingGroups[0].id", equalTo(AUTHORITY.getId()));
  }

  @Test
  void shouldReturnSettingCollection() {
    given().headers(INVENTORY_HEADERS).get(AUDIT_CONFIG_SETTINGS_PATH.formatted(INVENTORY.getId()))
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("totalRecords", equalTo(3))
      .body("settings[1].key", equalTo(RECORDS_PAGE_SIZE.getValue()))
      .body("settings[1].value", notNullValue())
      .body("settings[1].type", equalTo("INTEGER"))
      .body("settings[1].groupId", equalTo(INVENTORY.getId()))
      .body("settings[1].metadata.createdDate", notNullValue())
      .body("settings[1].metadata.updatedDate", notNullValue())
      .body("settings[1].metadata.createdByUserId", notNullValue())
      .body("settings[1].metadata.updatedByUserId", notNullValue())
      .body("settings[2].key", equalTo(RETENTION_PERIOD.getValue()))
      .body("settings[2].value", notNullValue())
      .body("settings[2].type", equalTo("INTEGER"))
      .body("settings[2].groupId", equalTo(INVENTORY.getId()))
      .body("settings[2].metadata.createdDate", notNullValue())
      .body("settings[2].metadata.updatedDate", notNullValue())
      .body("settings[2].metadata.createdByUserId", notNullValue())
      .body("settings[2].metadata.updatedByUserId", notNullValue());
  }

  @Test
  void shouldReturn403OnGetSettingCollection_whenNoGroupPermission() {
    var permsHeader = new Header(XOkapiHeaders.PERMISSIONS,
      "[\"audit.config.groups.settings.audit.circulation.collection.get\"]");
    var headers = new Headers(TENANT_HEADER, USER_HEADER, CONTENT_TYPE_HEADER, permsHeader);
    given().headers(headers).get(AUDIT_CONFIG_SETTINGS_PATH.formatted(INVENTORY.getId()))
      .then().log().all()
      .statusCode(HttpStatus.HTTP_FORBIDDEN.toInt())
      .assertThat()
      .body("errors[0].message",
        containsString("'audit.config.groups.settings.audit.inventory.collection.get' required"))
      .body("errors[0].code", equalTo("unauthorized"));
  }

  @ParameterizedTest
  @MethodSource("settingValues")
  void shouldUpdateSetting(String groupId, String key, Object updatedValue, SettingValueType type, Headers headers) {
    given().headers(headers).body(getSimpleSettingObject(groupId, key, updatedValue, type).encode())
      .put(AUDIT_CONFIG_SETTING_ENTRY_PATH.formatted(groupId, key)).then()
      .log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    var responseBody = given().headers(headers).get(AUDIT_CONFIG_SETTINGS_PATH.formatted(groupId))
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract().asString();
    var settingCollection = Json.decodeValue(responseBody, SettingCollection.class);
    for (var setting : settingCollection.getSettings()) {
      if (setting.getKey().equals(key) && setting.getGroupId().equals(groupId)) {
        assertThat(setting.getValue()).isEqualTo(updatedValue);
      }
    }
  }

  @Test
  void shouldReturn403OnUpdateSetting_whenNoSettingPermission() {
    var permsHeader = new Header(XOkapiHeaders.PERMISSIONS,
      "[\"audit.config.groups.settings.audit.circulation.collection.get\"]");
    var headers = new Headers(TENANT_HEADER, USER_HEADER, CONTENT_TYPE_HEADER, permsHeader);
    var body = getSimpleSettingObject(INVENTORY.getId(), RECORDS_PAGE_SIZE.getValue(), 50, INTEGER);
    given().headers(headers).body(body.encode())
      .put(AUDIT_CONFIG_SETTING_ENTRY_PATH.formatted(INVENTORY.getId(), RECORDS_PAGE_SIZE.getValue()))
      .then().log().all()
      .statusCode(HttpStatus.HTTP_FORBIDDEN.toInt())
      .assertThat()
      .body("errors[0].message",
        containsString("'audit.config.groups.settings.audit.inventory.records.page.size.item.put' required"))
      .body("errors[0].code", equalTo("unauthorized"));
  }

  private static Stream<Arguments> settingValues() {
    return Stream.of(
      Arguments.of(INVENTORY.getId(), RECORDS_PAGE_SIZE.getValue(), 50, INTEGER, INVENTORY_HEADERS),
      Arguments.of(AUTHORITY.getId(), RECORDS_PAGE_SIZE.getValue(), 20, INTEGER, AUTHORITY_HEADERS),
      Arguments.of(INVENTORY.getId(), RETENTION_PERIOD.getValue(), 1, INTEGER, INVENTORY_HEADERS),
      Arguments.of(AUTHORITY.getId(), RETENTION_PERIOD.getValue(), 3, INTEGER, AUTHORITY_HEADERS),
      Arguments.of(INVENTORY.getId(), ENABLED.getValue(), false, BOOLEAN, INVENTORY_HEADERS),
      Arguments.of(AUTHORITY.getId(), ENABLED.getValue(), false, BOOLEAN, AUTHORITY_HEADERS)
    );
  }

  private JsonObject getSimpleSettingObject(String groupId, String key, Object value, SettingValueType type) {
    return new JsonObject()
      .put("key", key)
      .put("value", value)
      .put("groupId", groupId)
      .put("type", type.value());
  }

}
