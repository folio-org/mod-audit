package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.TestSuite.isInitialized;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.TestBase;
import org.folio.TestSuite;
import org.folio.rest.tools.PomReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.restassured.http.Header;
import io.restassured.http.Headers;

public class ApiTestBase extends TestBase {
  protected static final Header TENANT = new Header("X-Okapi-Tenant", "modaudittest");
  protected static final Header PERMS = new Header("X-Okapi-Permissions", "audit.all");
  protected static final Header CONTENT_TYPE = new Header("Content-Type", "application/json");
  protected static final Headers HEADERS = new Headers(TENANT, PERMS, CONTENT_TYPE);

  public final String CIRCULATION_LOGS_ENDPOINT = "/audit-data/circulation/logs";

  protected static String moduleId;

  @BeforeAll
  public static void globalSetup() throws InterruptedException, ExecutionException, TimeoutException {
    Locale.setDefault(Locale.US);

    String moduleName = PomReader.INSTANCE.getModuleName()
      .replaceAll("_", "-");
    String moduleVersion = PomReader.INSTANCE.getVersion();
    moduleId = moduleName + "-" + moduleVersion;

    if (!isInitialized) {
      TestSuite.globalInitialize();
    }

    // initialize database and load sample data
    String tenants = "{\"module_to\":\"" + moduleId + "\"," + "\"parameters\": [ { \"key\":\"loadSample\", \"value\": true } ] }";
    given().headers(HEADERS)
      .body(tenants)
      .post("/_/tenant")
      .then()
      .log()
      .all()
      .statusCode(201);
  }

  @AfterAll
  public static void globalTearDown() {
    if (isInitialized) {
      TestSuite.globalTearDown();
    }
  }
}
