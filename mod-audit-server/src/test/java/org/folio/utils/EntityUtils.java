package org.folio.utils;

import java.util.Date;
import java.util.UUID;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEvent;

public class EntityUtils {

  public static String TENANT_ID = "diku";
  public static String PIECE_ID = "2cd4adc4-f287-49b6-a9c6-9eacdc4868e7";
  public static String ORDER_ID = "a21fc51c-d46b-439b-8c79-9b2be41b79a6";
  public static String ORDER_LINE_ID = "a22fc51c-d46b-439b-8c79-9b2be41b79a6";
  public static String INVOICE_ID = "550e8400-e29b-41d4-a716-446655440001";
  public static String INVOICE_LINE_ID = "550e8400-e29b-41d4-a716-446655440001";

  public static OrderAuditEvent createOrderAuditEvent(String id) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product 123 ");

    return new OrderAuditEvent()
      .withId(id)
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(ORDER_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot(jsonObject);
  }

  public static OrderAuditEvent createOrderAuditEventWithoutSnapshot() {
    return new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(ORDER_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderSnapshot("Test");
  }

  public static OrderLineAuditEvent createOrderLineAuditEvent(String id) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product");

    return new OrderLineAuditEvent()
      .withId(id)
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderId(ORDER_ID)
      .withOrderLineId(ORDER_LINE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrderLineSnapshot(jsonObject);
  }

  public static PieceAuditEvent createPieceAuditEvent(String id) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product");
    jsonObject.put("receivingStatus", "Expected");

    return new PieceAuditEvent()
      .withId(id)
      .withAction(PieceAuditEvent.Action.CREATE)
      .withPieceId(PIECE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withPieceSnapshot(jsonObject);
  }

  public static PieceAuditEvent createPieceAuditEvent(String id, int claimingInterval, String receivingStatus) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product");
    jsonObject.put("claimingInterval", claimingInterval);
    jsonObject.put("receivingStatus", receivingStatus);

    return new PieceAuditEvent()
      .withId(id)
      .withAction(PieceAuditEvent.Action.EDIT)
      .withPieceId(PIECE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withPieceSnapshot(jsonObject);
  }

  public static PieceAuditEvent createPieceAuditEventWithoutSnapshot() {
    return new PieceAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(PieceAuditEvent.Action.CREATE)
      .withPieceId(PIECE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withPieceSnapshot("Test");
  }

  public static InvoiceLineAuditEvent createInvoiceLineAuditEvent(String id) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Product");

    return new InvoiceLineAuditEvent()
      .withId(id)
      .withAction(InvoiceLineAuditEvent.Action.CREATE)
      .withInvoiceId(INVOICE_ID)
      .withInvoiceLineId(INVOICE_LINE_ID)
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withInvoiceLineSnapshot(jsonObject);
  }
}
