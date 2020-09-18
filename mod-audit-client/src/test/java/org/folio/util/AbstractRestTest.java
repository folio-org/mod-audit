package org.folio.util;

import static org.folio.rest.util.OkapiConnectionParams.OKAPI_TENANT_HEADER;
import static org.folio.rest.util.OkapiConnectionParams.OKAPI_URL_HEADER;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.LogEventPayload;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

/**
 * Abstract test for the REST API testing needs.
 */
public abstract class AbstractRestTest {

  public static final String TOKEN = "token";
  private static final String HTTP_PORT = "http.port";
  protected static final String TENANT_ID = "diku";
  public static final String PUBSUB_EVENT_TYPES_DECLARE_PUBLISHER_URL = "/pubsub/event-types/declare/publisher";
  public static final String PUBSUB_EVENT_TYPES = "/pubsub/event-types";
  protected static final String PUBSUB_PUBLISH_URL = "/pubsub/publish";
  public static final String LOCALHOST = "http://localhost:";
  public static int port;
  private static Vertx vertx;
  public static Map<String, String> okapiHeaders = new HashMap<>();

  protected static final String okapiUserIdHeader = UUID.randomUUID().toString();

  public static WireMockRule snapshotMockServer = new WireMockRule();

  @Before
  public void setUp() {
    okapiHeaders.put(OKAPI_URL_HEADER, LOCALHOST + snapshotMockServer.port());
    okapiHeaders.put(OKAPI_TENANT_HEADER, TENANT_ID);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, TOKEN);
    okapiHeaders.put(RestVerticle.OKAPI_USERID_HEADER, okapiUserIdHeader);
  }

  @BeforeClass
  public static void setUpClass(final TestContext context) throws IOException {
    vertx = Vertx.vertx();
    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx)
      .startEmbeddedPostgres();
    deployVerticle(context);
    snapshotMockServer.start();
  }

  @After
  public void tearDown() {
    snapshotMockServer.resetAll();
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> async.complete()));
  }

  private static void deployVerticle(final TestContext context) {
    Async async = context.async();
    port = NetworkUtils.nextFreePort();
    String okapiUrl = LOCALHOST + port;
    TenantClient tenantClient = new TenantClient(okapiUrl, TENANT_ID, TOKEN);
    final DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, deployVerticleAr -> {
      try {
        TenantAttributes tenantAttributes = new TenantAttributes();
        tenantAttributes.setModuleTo(PomReader.INSTANCE.getModuleName());
        tenantClient.postTenant(tenantAttributes, postTenantAr -> async.complete());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public static LogEventPayload buildEventSample() {
    return new LogEventPayload().withLoggedObjectType(LogEventPayload.LoggedObjectType.LOAN)
      .withBody("{request: body}");
  }
}
