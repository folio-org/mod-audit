package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.services.configuration.Setting.INVENTORY_RECORDS_PAGE_SIZE;
import static org.folio.services.configuration.SettingGroup.AUTHORITY;
import static org.folio.services.configuration.SettingGroup.INVENTORY;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.json.JsonObject;
import java.util.UUID;
import org.folio.HttpStatus;
import org.folio.okapi.common.XOkapiHeaders;
import org.junit.jupiter.api.Test;

public class AuditConfigAPITest extends ApiTestBase {

  private static final Header TENANT_HEADER = new Header(XOkapiHeaders.TENANT, "modaudittest");
  private static final Header PERMS_HEADER = new Header(XOkapiHeaders.PERMISSIONS, """
    ["audit.config.groups.settings.collection.get",
    "audit.config.groups.settings.audit.inventory.item.put",
    "audit.config.groups.settings.audit.inventory.collection.get"]""");
  private static final Header USER_HEADER = new Header(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  private static final Headers HEADERS = new Headers(TENANT_HEADER, PERMS_HEADER, USER_HEADER, CONTENT_TYPE_HEADER);

  private static final String AUDIT_CONFIG_GROUPS_PATH = "/audit/config/groups";
  private static final String AUDIT_CONFIG_SETTINGS_PATH = "/audit/config/groups/%s/settings";
  private static final String AUDIT_CONFIG_SETTING_ENTRY_PATH = "/audit/config/groups/%s/settings/%s";

  @Test
  void shouldReturnSettingGroupCollection() {
    given().headers(HEADERS).get(AUDIT_CONFIG_GROUPS_PATH)
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("totalRecords", equalTo(2))
      .body("settingGroups[0].id", equalTo(AUTHORITY.getId()));
  }

  @Test
  void shouldReturnSettingCollection() {
    given().headers(HEADERS).get(AUDIT_CONFIG_SETTINGS_PATH.formatted(INVENTORY.getId()))
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("totalRecords", equalTo(1))
      .body("settings[0].key", equalTo(INVENTORY_RECORDS_PAGE_SIZE.getKey()))
      .body("settings[0].value", notNullValue())
      .body("settings[0].type", equalTo("INTEGER"))
      .body("settings[0].groupId", equalTo(INVENTORY.getId()))
      .body("settings[0].metadata.createdDate", notNullValue())
      .body("settings[0].metadata.updatedDate", notNullValue())
      .body("settings[0].metadata.createdByUserId", notNullValue())
      .body("settings[0].metadata.updatedByUserId", notNullValue());
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

  @Test
  void shouldUpdateSetting() {
    given().headers(HEADERS).body(getSimpleSettingObject().encode())
      .put(AUDIT_CONFIG_SETTING_ENTRY_PATH.formatted(INVENTORY.getId(), INVENTORY_RECORDS_PAGE_SIZE.getKey())).then()
      .log().all()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    given().headers(HEADERS).get(AUDIT_CONFIG_SETTINGS_PATH.formatted(INVENTORY.getId()))
      .then().log().all()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .assertThat()
      .body("totalRecords", equalTo(1))
      .body("settings[0].key", equalTo("records.page.size"))
      .body("settings[0].value", equalTo(50));
  }

  @Test
  void shouldReturn403OnUpdateSetting_whenNoGroupPermission() {
    var permsHeader = new Header(XOkapiHeaders.PERMISSIONS,
      "[\"audit.config.groups.settings.audit.circulation.collection.get\"]");
    var headers = new Headers(TENANT_HEADER, USER_HEADER, CONTENT_TYPE_HEADER, permsHeader);
    given().headers(headers).body(getSimpleSettingObject().encode())
      .put(AUDIT_CONFIG_SETTING_ENTRY_PATH.formatted(INVENTORY.getId(), INVENTORY_RECORDS_PAGE_SIZE.getKey()))
      .then().log().all()
      .statusCode(HttpStatus.HTTP_FORBIDDEN.toInt())
      .assertThat()
      .body("errors[0].message",
        containsString("'audit.config.groups.settings.audit.inventory.item.put' required"))
      .body("errors[0].code", equalTo("unauthorized"));
  }

  private JsonObject getSimpleSettingObject() {
    return new JsonObject()
      .put("key", "records.page.size")
      .put("value", 50)
      .put("groupId", INVENTORY.getId())
      .put("type", "INTEGER");
  }

}
