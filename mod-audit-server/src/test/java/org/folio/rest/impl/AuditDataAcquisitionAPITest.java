package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.utils.EntityUtils.ORDER_ID;
import static org.folio.utils.EntityUtils.ORDER_LINE_ID;
import static org.folio.utils.EntityUtils.PIECE_ID;
import static org.folio.utils.EntityUtils.createPieceAuditEvent;
import static org.hamcrest.Matchers.containsString;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.dao.acquisition.impl.PieceEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class AuditDataAcquisitionAPITest extends ApiTestBase {

  private static final Header TENANT = new Header("X-Okapi-Tenant", "modaudittest");
  private static final Header PERMS = new Header("X-Okapi-Permissions", "audit.all");
  private static final Header CONTENT_TYPE = new Header("Content-Type", "application/json");
  private static final String INVALID_ID = "646ea52c-2c65-4d28-9a8f-e0d200fd6b00";
  private static final String ACQ_AUDIT_ORDER_PATH = "/audit-data/acquisition/order/";
  private static final String ACQ_AUDIT_ORDER_LINE_PATH = "/audit-data/acquisition/order-line/";
  private static final String ACQ_AUDIT_PIECE_PATH = "/audit-data/acquisition/piece/";
  private static final String ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH = "/status-change-history";
  private static final String TENANT_ID = "modaudittest";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  OrderEventsDaoImpl orderEventDao;
  @InjectMocks
  OrderLineEventsDaoImpl orderLineEventDao;
  @InjectMocks
  PieceEventsDaoImpl pieceEventsDao;

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

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ INVALID_ID)
      .then().log().all().statusCode(200)
      .body(containsString("orderLineAuditEvents")).body(containsString("totalItems"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ ORDER_LINE_ID)
      .then().log().all().statusCode(200)
      .body(containsString(ORDER_LINE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ ORDER_LINE_ID +"?limit=1").then().log().all().statusCode(200)
      .body(containsString(ORDER_LINE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH+ ORDER_LINE_ID +"?sortBy=action_date").then().log().all().statusCode(200)
      .body(containsString(ORDER_LINE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ ORDER_LINE_ID + 123).then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }

  @Test
  void shouldReturnPieceEventsOnGetByPieceId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","Test Product2");

    PieceAuditEvent pieceAuditEvent = new PieceAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(PieceAuditEvent.Action.CREATE)
      .withPieceId(PIECE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withPieceSnapshot(jsonObject);

    pieceEventsDao.save(pieceAuditEvent, TENANT_ID);

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + INVALID_ID)
      .then().log().all().statusCode(200)
      .body(containsString("pieceAuditEvents")).body(containsString("totalItems"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID)
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID +"?limit=1")
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + "?sortBy=action_date")
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + 123).then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }

  @Test
  void shouldReturnPieceEventsStatusChangeHistoryGetByPieceId() {
    String id1 = UUID.randomUUID().toString();
    String id2 = UUID.randomUUID().toString();
    String id3 = UUID.randomUUID().toString();
    String id4 = UUID.randomUUID().toString();
    String id5 = UUID.randomUUID().toString();
    var pieceAuditEvent1 = createPieceAuditEvent(id1, "STATUS 1");
    var pieceAuditEvent2 = createPieceAuditEvent(id2, "STATUS 1");
    var pieceAuditEvent3 = createPieceAuditEvent(id3, "STATUS 2");
    var pieceAuditEvent4 = createPieceAuditEvent(id4, "STATUS 2");
    var pieceAuditEvent5 = createPieceAuditEvent(id5, "STATUS 1");
    var localDateTime1 = LocalDateTime.of(2023, 4, 20, 6, 9, 30);
    var localDateTime2 = LocalDateTime.of(2023, 4, 20, 6, 10, 30);
    var localDateTime3 = LocalDateTime.of(2023, 4, 20, 6, 11, 30);
    var localDateTime4 = LocalDateTime.of(2023, 4, 20, 6, 12, 30);
    var localDateTime5 = LocalDateTime.of(2023, 4, 20, 6, 13, 30);
    pieceAuditEvent1.setActionDate(Date.from(localDateTime1.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent2.setActionDate(Date.from(localDateTime2.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent3.setActionDate(Date.from(localDateTime3.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent4.setActionDate(Date.from(localDateTime4.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent5.setActionDate(Date.from(localDateTime5.atZone(ZoneId.systemDefault()).toInstant()));

    pieceEventsDao.save(pieceAuditEvent1, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent2, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent3, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent4, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent5, TENANT_ID);

    // based on our business logic, it returns pieceAuditEvent1, pieceAuditEvent3, pieceAuditEvent5
    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + INVALID_ID + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH)
      .then().log().all().statusCode(200)
      .body(containsString("pieceAuditEvents")).body(containsString("totalItems"));


    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH)
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID))
      .body(containsString(id1))
      .body(containsString(id3))
      .body(containsString(id5));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH +"?limit=1")
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID))
      .body(containsString(id1));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH +"?sortBy=action_date")
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID))
      .body(containsString(id1))
      .body(containsString(id3))
      .body(containsString(id5));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH + 123)
      .then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }
}
