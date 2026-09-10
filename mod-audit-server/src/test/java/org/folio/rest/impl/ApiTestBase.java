package org.folio.rest.impl;

import static org.folio.TestSuite.isInitialized;
import static org.folio.utils.TenantApiTestUtil.deleteTenantAndPurgeTables;
import static org.folio.utils.TenantApiTestUtil.prepareTenant;

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
  protected static final Header PERMS = new Header("X-Okapi-Permissions", "audit.all");
  protected static final Header CONTENT_TYPE = new Header("Content-Type", "application/json");

  public static final String CIRCULATION_LOGS_ENDPOINT = "/audit-data/circulation/logs";

  private static TenantJob tenantJob;

  /**
   * Built lazily (not a static final constant) because {@link TestSuite#port} is a fresh, dynamically
   * allocated port per {@code @Nested} test class run; a static final field would capture a stale port
   * from whichever class triggered this class's static initialization first.
   */
  public static Header okapiUrl() {
    return new Header("X-Okapi-Url", "http://localhost:" + TestSuite.port);
  }

  public static Headers headers() {
    return new Headers(TENANT, PERMS, CONTENT_TYPE, okapiUrl());
  }

  @BeforeAll
  public static void globalSetup() throws InterruptedException, ExecutionException, TimeoutException {
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
