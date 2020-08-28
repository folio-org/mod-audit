package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static io.restassured.RestAssured.*;
import static org.folio.rest.impl.AuditDataImpl.API_CXT;
import static org.hamcrest.Matchers.containsString;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Interface test for mod-audit.
 */

class AuditDataImplTest extends TestBase {
  private final Logger logger = LoggerFactory.getLogger(TestBase.class);
  // test data
  JsonObject audit = new JsonObject().put("tenant", "diku");
  JsonObject badAudit = new JsonObject().put("x", "y");
  String nonExistingId = UUID.randomUUID().toString();
  String badId = nonExistingId + "1";

  @Test
  void auditDataTests() {
    logger.info("Audit data test starting");

    // health check
    given().header(ctype).get("/admin/health").then().log().all().statusCode(200);

    // get collection
    given().header(ctype).header(tenant).header(perms).get(API_CXT).then().log().all().statusCode(200)
      .body(containsString("\"audit\" : [ ]"));

    // create with valid content
    String id = given().header(ctype).header(tenant).header(perms).body(Json.encode(audit)).post(API_CXT).then()
      .statusCode(201).extract().header("Location");
    id = id.substring(id.lastIndexOf("/") + 1);
    // create with invalid content - non JSON
    given().header(ctype).header(tenant).header(ctype).body("This is not json").post(API_CXT).then().log().all()
      .statusCode(400).body(containsString("Json content error"));
    // create with invalid content - missing field
    given().header(ctype).header(tenant).header(ctype).body(Json.encode(badAudit)).post(API_CXT).then().log().all()
      .statusCode(422);

    // get by id
    given().header(ctype).header(tenant).header(perms).get(API_CXT + "/" + id).then().log().all().statusCode(200)
      .body(containsString("diku")).body(containsString(id));

    // get by non-existing id
    given().header(ctype).header(tenant).header(perms).get(API_CXT + "/" + nonExistingId).then().log().all()
      .statusCode(404);
    // get by bad id
    given().header(ctype).header(tenant).header(perms).get(API_CXT + "/" + badId).then().log().all().statusCode(400);

    // update by id
    audit.put("id", id).put("tenant", "diku2");
    given().header(ctype).header(tenant).header(perms).body(Json.encode(audit)).put(API_CXT + "/" + id).then()
      .statusCode(204);
    // verify update
    given().header(ctype).header(tenant).header(perms).get(API_CXT + "/" + id).then().log().all().log()
      .ifValidationFails().statusCode(200).body(containsString("diku2")).body(containsString(id));
    // update with conflict ids
    given().header(ctype).header(tenant).body(Json.encode(audit)).put(API_CXT + "/" + nonExistingId).then().log().all()
      .statusCode(422);
    // update by non-existing id
    audit.put("id", nonExistingId);
    given().header(ctype).header(tenant).body(Json.encode(audit)).put(API_CXT + "/" + nonExistingId).then().log().all()
      .statusCode(404);
    // update by bad id
    audit.put("id", badId);
    given().header(ctype).header(tenant).body(Json.encode(audit)).put(API_CXT + "/" + badId).then().log().all()
      .statusCode(400);
    // restore id
    audit.put("id", id);

    // delete by id
    given().header(ctype).header(tenant).header(perms).delete(API_CXT + "/" + id).then().log().all().statusCode(204);
    // verify delete
    given().header(ctype).header(tenant).header(perms).get(API_CXT + "/" + id).then().log().all().statusCode(404);
    // delete by non-existing id
    given().header(ctype).header(tenant).header(perms).delete(API_CXT + "/" + nonExistingId).then().log().all()
      .statusCode(404);
    // delete by bad id
    given().header(ctype).header(tenant).header(perms).delete(API_CXT + "/" + badId).then().log().all().statusCode(400);
  }
}
