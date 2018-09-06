package org.folio.rest.impl;

import java.util.Locale;
import java.util.UUID;

import org.junit.Test;

import static io.restassured.RestAssured.*;
import static org.folio.rest.impl.AuditDataResourceImpl.API_CXT;
import static org.hamcrest.Matchers.containsString;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.junit.After;

/**
 * Interface test for mod-audit-storage.
 */
@RunWith(VertxUnitRunner.class)
public class AuditDataResourceImplTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final int port = Integer.parseInt(System.getProperty("port", "8081"));

  private final Header tenant = new Header("X-Okapi-Tenant", "modaudittest");
  private final Header perms = new Header("X-Okapi-Permissions", "audit.all");
  private final Header ctype = new Header("Content-Type", "application/json");

  private String moduleName;
  private String moduleVersion;
  private String moduleId;

  private Vertx vertx;
  private Async async;

  // test data
  JsonObject audit = new JsonObject().put("tenant", "diku");
  JsonObject badAudit = new JsonObject().put("x", "y");
  String nonExistingId = UUID.randomUUID().toString();
  String badId = nonExistingId + "1";

  @Before
  public void setUp(TestContext context) {
    Locale.setDefault(Locale.US);
    vertx = Vertx.vertx();
    moduleName = PomReader.INSTANCE.getModuleName().replaceAll("_", "-");
    moduleVersion = PomReader.INSTANCE.getVersion();
    moduleId = moduleName + "-" + moduleVersion;
    logger.info("Test setup starting for " + moduleId);
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    } catch (IOException e) {
      e.printStackTrace();
      context.fail(e);
      return;
    }

    JsonObject conf = new JsonObject().put("http.port", port).put(HttpClientMock2.MOCK_MODE, "true");
    DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    vertx.deployVerticle(RestVerticle.class.getName(), opt, context.asyncAssertSuccess());
    RestAssured.port = port;
    logger.info("Setup done. Using port " + port);
  }

  @After
  public void tearDown(TestContext context) {
    logger.info("Cleaning up after ModuleTest");
    async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Test
  public void tests(TestContext context) {
    async = context.async();
    logger.info("Test starting");

    // health check
    given().header(ctype).get("/admin/health").then().log().all().log().all().statusCode(200);

    // without tenant
    given().header(ctype).get(API_CXT).then().log().all().statusCode(400).body(containsString("Tenant"));

    // initialize database
    String tenants = "{\"module_to\":\"" + moduleId + "\"}";
    given().header(ctype).header(tenant).header(ctype).body(tenants).post("/_/tenant").then().log().all()
        .statusCode(201);

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

    // All done
    async.complete();
  }

}
