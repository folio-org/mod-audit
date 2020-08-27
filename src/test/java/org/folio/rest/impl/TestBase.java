package org.folio.rest.impl;

import static org.folio.TestSuite.isInitialized;

import io.restassured.http.Header;
import io.vertx.junit5.VertxExtension;
import org.folio.TestSuite;
import org.folio.rest.tools.PomReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@ExtendWith(VertxExtension.class)
public class TestBase {
  protected final Header tenant = new Header("X-Okapi-Tenant", "modaudittest");
  protected final Header perms = new Header("X-Okapi-Permissions", "audit.all");
  protected final Header ctype = new Header("Content-Type", "application/json");

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
  }

  @AfterAll
  public static void globalTearDown() {
    if (isInitialized) {
      TestSuite.globalTearDown();
    }
  }
}
