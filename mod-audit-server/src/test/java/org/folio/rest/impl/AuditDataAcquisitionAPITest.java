package org.folio.rest.impl;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;


import java.util.Date;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AuditDataAcquisitionAPITest extends ApiTestBase {

  public static final Header TENANT = new Header("X-Okapi-Tenant", "modaudittest");

  protected static final Header PERMS = new Header("X-Okapi-Permissions", "audit.all");

  protected static final Header CONTENT_TYPE = new Header("Content-Type", "application/json");

  protected static final String INVALID_ID = "646ea52c-2c65-4d28-9a8f-e0d200fd6b00";

  protected static final String ACQ_AUDIT_ORDER_PATH = "/audit-data/acquisition/order/";

  protected static final String ACQ_AUDIT_ORDER_LINE_PATH = "/audit-data/acquisition/order-line/";

  private static final String TENANT_ID = "modaudittest";

  public static final String ORDER_ID = "a21fc51c-d46b-439b-8c79-9b2be41b79a6";

  public static final String ORDER_LINE_ID = "a22fc51c-d46b-439b-8c79-9b2be41b79a6";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  OrderEventsDaoImpl orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  OrderLineEventsDaoImpl orderLineEventDao = new OrderLineEventsDaoImpl(postgresClientFactory);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);
    orderLineEventDao = new OrderLineEventsDaoImpl(postgresClientFactory);

  }

  @Test
  void shouldReturnOrderEventsOnGetByOrderId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product2");

    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(ORDER_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);

    orderEventDao.save(orderAuditEvent, TENANT_ID);

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ INVALID_ID).then().log().all().statusCode(200)
      .body(containsString("orderAuditEvents")).body(containsString("totalItems"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ ORDER_ID).then().log().all().statusCode(200)
      .body(containsString(ORDER_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ ORDER_ID +"?limit=1").then().log().all().statusCode(200)
      .body(containsString(ORDER_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ ORDER_ID + 123).then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }

  @Test
  void shouldReturnOrderLineEventsOnGetByOrderLineId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product2");

    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(ORDER_ID)
      .withOrderLineId(ORDER_LINE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderLineSnapshot(jsonObject);

    orderLineEventDao.save(orderLineAuditEvent, TENANT_ID);

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ INVALID_ID).then().log().all().statusCode(200)
      .body(containsString("orderLineAuditEvents")).body(containsString("totalItems"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ ORDER_LINE_ID).then().log().all().statusCode(200)
      .body(containsString(ORDER_LINE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ ORDER_LINE_ID +"?limit=1").then().log().all().statusCode(200)
      .body(containsString(ORDER_LINE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ ORDER_LINE_ID +"?sortBy=action_date").then().log().all().statusCode(200)
      .body(containsString(ORDER_LINE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ ORDER_LINE_ID + 123).then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }
}
