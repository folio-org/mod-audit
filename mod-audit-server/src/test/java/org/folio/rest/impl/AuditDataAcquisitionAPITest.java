package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.utils.EntityUtils.ORDER_ID;
import static org.folio.utils.EntityUtils.ORDER_LINE_ID;
import static org.folio.utils.EntityUtils.ORGANIZATION_ID;
import static org.folio.utils.EntityUtils.PIECE_ID;
import static org.folio.utils.EntityUtils.INVOICE_ID;
import static org.folio.utils.EntityUtils.createPieceAuditEvent;
import static org.hamcrest.Matchers.containsString;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.impl.InvoiceEventsDaoImpl;
import org.folio.dao.acquisition.impl.InvoiceLineEventsDaoImpl;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.dao.acquisition.impl.OrganizationEventsDaoImpl;
import org.folio.dao.acquisition.impl.PieceEventsDaoImpl;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
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
  private static final String ACQ_AUDIT_INVOICE_PATH = "/audit-data/acquisition/invoice/";
  private static final String ACQ_AUDIT_INVOICE_LINE_PATH = "/audit-data/acquisition/invoice-line/";
  private static final String ACQ_AUDIT_ORGANIZATION_PATH = "/audit-data/acquisition/organization/";
  private static final String TENANT_ID = "modaudittest";

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  OrderEventsDaoImpl orderEventDao;
  @InjectMocks
  OrderLineEventsDaoImpl orderLineEventDao;
  @InjectMocks
  PieceEventsDaoImpl pieceEventsDao;
  @InjectMocks
  InvoiceEventsDaoImpl invoiceEventsDao;
  @InjectMocks
  InvoiceLineEventsDaoImpl invoiceLineEventsDao;
  @InjectMocks
  OrganizationEventsDaoImpl organizationEventsDao;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);
    orderLineEventDao = new OrderLineEventsDaoImpl(postgresClientFactory);
    invoiceEventsDao = new InvoiceEventsDaoImpl(postgresClientFactory);
    invoiceLineEventsDao = new InvoiceLineEventsDaoImpl(postgresClientFactory);
    organizationEventsDao = new OrganizationEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldReturnOrderEventsOnGetByOrderId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product2");

    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(ORDER_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);

    orderEventDao.save(orderAuditEvent, TENANT_ID);

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH + INVALID_ID).then().log().all().statusCode(200)
      .body(containsString("orderAuditEvents")).body(containsString("totalItems"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH + ORDER_ID).then().log().all().statusCode(200)
      .body(containsString(ORDER_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH + ORDER_ID + "?limit=1").then().log().all().statusCode(200)
      .body(containsString(ORDER_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH + ORDER_ID + 123).then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }

  @Test
  void shouldReturnOrderLineEventsOnGetByOrderLineId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product2");

    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(ORDER_ID)
      .withOrderLineId(ORDER_LINE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderLineSnapshot(jsonObject);

    orderLineEventDao.save(orderLineAuditEvent, TENANT_ID).onComplete(v -> {
      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH + INVALID_ID)
        .then().log().all().statusCode(200)
        .body(containsString("orderLineAuditEvents")).body(containsString("totalItems"));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH + ORDER_LINE_ID)
        .then().log().all().statusCode(200)
        .body(containsString(ORDER_LINE_ID));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH + ORDER_LINE_ID + "?limit=1").then().log().all().statusCode(200)
        .body(containsString(ORDER_LINE_ID));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_LINE_PATH + ORDER_LINE_ID + "?sortBy=action_date").then().log().all().statusCode(200)
        .body(containsString(ORDER_LINE_ID));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH + ORDER_LINE_ID + 123).then().log().all().statusCode(500)
        .body(containsString("UUID string too large"));
    });
  }

  @Test
  void shouldReturnPieceEventsOnGetByPieceId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product2");

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
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + "?limit=1")
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
  void shouldReturnPieceEventsStatusChangesHistoryGetByPieceId() {
    String id1 = UUID.randomUUID().toString();
    String id2 = UUID.randomUUID().toString();
    String id3 = UUID.randomUUID().toString();
    String id4 = UUID.randomUUID().toString();
    String id5 = UUID.randomUUID().toString();
    String id6 = UUID.randomUUID().toString();
    String id7 = UUID.randomUUID().toString();
    String id8 = UUID.randomUUID().toString();
    String id9 = UUID.randomUUID().toString();
    var pieceAuditEvent1 = createPieceAuditEvent(id1);
    var pieceAuditEvent2 = createPieceAuditEvent(id2, 3, "Claim sent"); // claiming interval has changed
    var pieceAuditEvent3 = createPieceAuditEvent(id3, 44, "Claim sent"); // claiming interval has changed
    var pieceAuditEvent4 = createPieceAuditEvent(id4, 44, "Claim sent");
    var pieceAuditEvent5 = createPieceAuditEvent(id5, 16, "Claim sent"); // claiming interval has changed
    var pieceAuditEvent6 = createPieceAuditEvent(id6, 16, "Received"); // receivingStatus has changed
    var pieceAuditEvent7 = createPieceAuditEvent(id7, 16, "Received");
    var pieceAuditEventWithDifferentPiece1 = createPieceAuditEvent(id8, 55, "Claim sent");
    var pieceAuditEventWithDifferentPiece2 = createPieceAuditEvent(id9, 9, "Claim sent");
    pieceAuditEventWithDifferentPiece1.setPieceId(UUID.randomUUID().toString());
    pieceAuditEventWithDifferentPiece2.setPieceId(UUID.randomUUID().toString());
    var localDateTime1 = LocalDateTime.of(2023, 4, 20, 6, 9, 30);
    var localDateTime2 = LocalDateTime.of(2023, 4, 20, 6, 10, 30);
    var localDateTime3 = LocalDateTime.of(2023, 4, 20, 6, 11, 30);
    var localDateTime4 = LocalDateTime.of(2023, 4, 20, 6, 12, 30);
    var localDateTime5 = LocalDateTime.of(2023, 4, 20, 6, 13, 30);
    var localDateTime6 = LocalDateTime.of(2023, 4, 20, 6, 14, 30);
    var localDateTime7 = LocalDateTime.of(2023, 4, 20, 6, 15, 30);
    var localDateTime8 = LocalDateTime.of(2023, 4, 20, 6, 9, 20);
    var localDateTime9 = LocalDateTime.of(2023, 4, 20, 6, 10, 20);
    pieceAuditEvent1.setActionDate(Date.from(localDateTime1.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent2.setActionDate(Date.from(localDateTime2.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent3.setActionDate(Date.from(localDateTime3.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent4.setActionDate(Date.from(localDateTime4.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent5.setActionDate(Date.from(localDateTime5.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent6.setActionDate(Date.from(localDateTime6.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEvent7.setActionDate(Date.from(localDateTime7.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEventWithDifferentPiece1.setActionDate(Date.from(localDateTime8.atZone(ZoneId.systemDefault()).toInstant()));
    pieceAuditEventWithDifferentPiece2.setActionDate(Date.from(localDateTime9.atZone(ZoneId.systemDefault()).toInstant()));

    pieceEventsDao.save(pieceAuditEvent1, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent2, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent3, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent4, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent5, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent6, TENANT_ID);
    pieceEventsDao.save(pieceAuditEvent7, TENANT_ID);

    pieceEventsDao.save(pieceAuditEventWithDifferentPiece1, TENANT_ID);
    pieceEventsDao.save(pieceAuditEventWithDifferentPiece2, TENANT_ID);
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
      .body(containsString(id2))
      .body(containsString(id3))
      .body(containsString(id5))
      .body(containsString(id6));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH + "?limit=1")
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH + "?sortBy=action_date")
      .then().log().all().statusCode(200)
      .body(containsString(PIECE_ID))
      .body(containsString(id2))
      .body(containsString(id3))
      .body(containsString(id5))
      .body(containsString(id6));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_PIECE_PATH + PIECE_ID + 123 + ACQ_AUDIT_PIECE_STATUS_CHANGE_HISTORY_PATH)
      .then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }

  @Test
  @CopilotGenerated
  void shouldReturnInvoiceEventsOnGetByInvoiceId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product2");

    InvoiceAuditEvent invoiceAuditEvent = new InvoiceAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(InvoiceAuditEvent.Action.CREATE)
      .withInvoiceId(INVOICE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withInvoiceSnapshot(jsonObject);

    invoiceEventsDao.save(invoiceAuditEvent, TENANT_ID);

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_INVOICE_PATH + INVALID_ID)
      .then().log().all().statusCode(200)
      .body(containsString("invoiceAuditEvents")).body(containsString("totalItems"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_INVOICE_PATH + INVOICE_ID)
      .then().log().all().statusCode(200)
      .body(containsString(INVOICE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_INVOICE_PATH + INVOICE_ID + "?limit=1")
      .then().log().all().statusCode(200)
      .body(containsString(INVOICE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_INVOICE_PATH + INVOICE_ID + "?sortBy=action_date")
      .then().log().all().statusCode(200)
      .body(containsString(INVOICE_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_INVOICE_PATH + INVOICE_ID + 123)
      .then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }

  @Test
  @CopilotGenerated
  void shouldReturnInvoiceLineEventsOnGetByInvoiceLineId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product2");

    InvoiceLineAuditEvent invoiceLineAuditEvent = new InvoiceLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(InvoiceLineAuditEvent.Action.CREATE)
      .withInvoiceId(UUID.randomUUID().toString())
      .withInvoiceLineId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withInvoiceLineSnapshot(jsonObject);

    invoiceLineEventsDao.save(invoiceLineAuditEvent, TENANT_ID).onComplete(v -> {
      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_INVOICE_LINE_PATH + INVALID_ID)
        .then().log().all().statusCode(200)
        .body(containsString("invoiceLineAuditEvents")).body(containsString("totalItems"));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_INVOICE_LINE_PATH + invoiceLineAuditEvent.getInvoiceLineId())
        .then().log().all().statusCode(200)
        .body(containsString(invoiceLineAuditEvent.getInvoiceLineId()));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_INVOICE_LINE_PATH + invoiceLineAuditEvent.getInvoiceLineId() + "?limit=1")
        .then().log().all().statusCode(200)
        .body(containsString(invoiceLineAuditEvent.getInvoiceLineId()));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_INVOICE_LINE_PATH + invoiceLineAuditEvent.getInvoiceLineId() + "?sortBy=action_date")
        .then().log().all().statusCode(200)
        .body(containsString(invoiceLineAuditEvent.getInvoiceLineId()));

      given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_INVOICE_LINE_PATH + invoiceLineAuditEvent.getInvoiceLineId() + 123)
        .then().log().all().statusCode(500)
        .body(containsString("UUID string too large"));
    });
  }

  @Test
  @CopilotGenerated
  void shouldReturnOrganizationEventsOnGetByOrganizationId() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product2");

    OrganizationAuditEvent organizationAuditEvent = new OrganizationAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrganizationAuditEvent.Action.CREATE)
      .withOrganizationId(ORGANIZATION_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrganizationSnapshot(jsonObject);

    organizationEventsDao.save(organizationAuditEvent, TENANT_ID);

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_ORGANIZATION_PATH + INVALID_ID)
      .then().log().all().statusCode(200)
      .body(containsString("organizationAuditEvents")).body(containsString("totalItems"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_ORGANIZATION_PATH + ORGANIZATION_ID)
      .then().log().all().statusCode(200)
      .body(containsString(ORGANIZATION_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_ORGANIZATION_PATH + ORGANIZATION_ID + "?limit=1")
      .then().log().all().statusCode(200)
      .body(containsString(ORGANIZATION_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_ORGANIZATION_PATH + ORGANIZATION_ID + "?sortBy=action_date")
      .then().log().all().statusCode(200)
      .body(containsString(ORGANIZATION_ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS)
      .get(ACQ_AUDIT_ORGANIZATION_PATH + ORGANIZATION_ID + 123)
      .then().log().all().statusCode(500)
      .body(containsString("UUID string too large"));
  }
}
