package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.TestSuite.isInitialized;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.junit5.VertxExtension;
import org.folio.rest.tools.PomReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@ExtendWith(VertxExtension.class)
public class TestBase {
  protected static final Header tenant = new Header("X-Okapi-Tenant", "modaudittest");
  protected static final Header perms = new Header("X-Okapi-Permissions", "audit.all");
  protected static final Header ctype = new Header("Content-Type", "application/json");
  protected static final Headers headers = new Headers(tenant, perms, ctype);

  protected static String moduleId;

  @BeforeAll
  public static void globalSetup() throws InterruptedException, ExecutionException, TimeoutException {
    Locale.setDefault(Locale.US);

    String moduleName = PomReader.INSTANCE.getModuleName().replaceAll("_", "-");
    String moduleVersion = PomReader.INSTANCE.getVersion();
    moduleId = moduleName + "-" + moduleVersion;

    if (!isInitialized) {
      TestSuite.globalInitialize();
    }

    // initialize database and load sample data
    String tenants = "{\"module_to\":\"" + moduleId + "\"," +
      "\"parameters\": [ { \"key\":\"loadSample\", \"value\": true } ] }";
    given().headers(headers).body(tenants).post("/_/tenant").then().log().all().statusCode(201);
  }

  @AfterAll
  public static void globalTearDown() {
    // delete tenant
    given().headers(headers).delete("/_/tenant").then().log().all().statusCode(204);

    if (isInitialized) {
      TestSuite.globalTearDown();
    }
  }
}
