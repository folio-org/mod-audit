package org.folio.rest.impl;

import static org.folio.TestSuite.isInitialized;
import static org.folio.utils.TenantApiTestUtil.deleteTenantAndPurgeTables;
import static org.folio.utils.TenantApiTestUtil.prepareTenant;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.TestSuite;
import org.folio.rest.jaxrs.model.TenantJob;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.restassured.http.Header;
import io.restassured.http.Headers;

public class ApiTestBase {
  public static final Header TENANT = new Header("X-Okapi-Tenant", "modaudittest");
  protected static final Header OKAPI_URL = new Header("X-Okapi-Url", "http://localhost");
  protected static final Header PERMS = new Header("X-Okapi-Permissions", "audit.all");
  protected static final Header CONTENT_TYPE = new Header("Content-Type", "application/json");
  protected static final Headers HEADERS = new Headers(TENANT, PERMS, CONTENT_TYPE, OKAPI_URL);

  public final String CIRCULATION_LOGS_ENDPOINT = "/audit-data/circulation/logs";

  private static TenantJob tenantJob;

  @BeforeAll
  public static void globalSetup() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    Locale.setDefault(Locale.US);

    if (!isInitialized) {
      TestSuite.globalInitialize();
    }

    tenantJob = prepareTenant(TENANT, true, false);
  }

  @AfterAll
  public static void globalTearDown() {
    deleteTenantAndPurgeTables(TENANT);

    if (isInitialized) {
      TestSuite.globalTearDown();
    }
  }
}
