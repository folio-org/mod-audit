package org.folio.utils;

import io.vertx.core.json.JsonObject;
import org.folio.dao.configuration.SettingEntity;
import org.folio.dao.configuration.SettingValueType;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.domain.diff.ChangeType;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.util.marc.EventMetadata;
import org.folio.util.marc.MarcEventPayload;
import org.folio.util.marc.Record;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordDomainEventType;
import org.folio.util.marc.SourceRecordType;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityUtils {

  public static final String ACTION_DATE_SORT_BY = "action_date";
  public static final String DESC_ORDER = "desc";
  public static final int LIMIT = 1;
  public static final int OFFSET = 1;
  public static final String TENANT_ID = "diku";
  public static final String PIECE_ID = "2cd4adc4-f287-49b6-a9c6-9eacdc4868e7";
  public static final String ORDER_ID = "a21fc51c-d46b-439b-8c79-9b2be41b79a6";
  public static final String ORDER_LINE_ID = "3448efa3-ef4b-4518-a924-d1b36255cc20";
  public static final String INVOICE_ID = "3f29b1a4-8c2b-4d3a-9b1e-5f2a1b4c8d3a";
  public static final String INVOICE_LINE_ID = "550e8400-e29b-41d4-a716-446655440001";
  public static final String ORGANIZATION_ID = "39e7362e-b487-4d51-8bdb-cbd6bf29d1c7";
  public static final String SOURCE_RECORD_ID = UUID.randomUUID().toString();
  public static final String USER_ID = UUID.randomUUID().toString();

  public static final String LEADER_OLD = "old-leader";
  public static final String LEADER_NEW = "updated-leader";
  public static final String FIELD_001 = "001";
  public static final String FIELD_245 = "245";
  public static final String TITLE_OLD = "Old Title";
  public static final String TITLE_NEW = "Updated Title";
  public static final String VALUE_001 = "in0000000";

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

  public static InvoiceAuditEvent createInvoiceAuditEvent(String id) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Invoice 123");

    return new InvoiceAuditEvent()
      .withId(id)
      .withAction(InvoiceAuditEvent.Action.CREATE)
      .withInvoiceId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withInvoiceSnapshot(jsonObject);
  }

  public static InvoiceAuditEvent createInvoiceAuditEventWithoutSnapshot() {
    return new InvoiceAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(InvoiceAuditEvent.Action.CREATE)
      .withInvoiceId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withInvoiceSnapshot("Test");
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


  public static OrganizationAuditEvent createOrganizationAuditEvent(String id) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name", "Test Organization 123");

    return new OrganizationAuditEvent()
      .withId(id)
      .withAction(OrganizationAuditEvent.Action.CREATE)
      .withOrganizationId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrganizationSnapshot(jsonObject);
  }

  public static OrganizationAuditEvent createOrganizationAuditEventWithoutSnapshot() {
    return new OrganizationAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrganizationAuditEvent.Action.CREATE)
      .withOrganizationId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withActionDate(new Date())
      .withOrganizationSnapshot("Test");
  }

  public static InventoryAuditEntity createInventoryAuditEntity() {
    var changeRecordDto = new ChangeRecordDto();
    changeRecordDto.setFieldChanges(List.of(new FieldChangeDto(ChangeType.MODIFIED, "id", "id", "old", "new")));

    return new InventoryAuditEntity(UUID.randomUUID(), Timestamp.from(Instant.now()), UUID.randomUUID(), "action",
      UUID.randomUUID(), changeRecordDto);
  }

  public static SettingEntity createSettingEntity() {
    return SettingEntity.builder()
      .type(SettingValueType.INTEGER)
      .value(10)
      .build();
  }

  public static InventoryEvent createInventoryEvent(String eventId, InventoryEventType type,
                                                    InventoryResourceType resourceType) {
    return createInventoryEvent(eventId, type, resourceType, false);
  }

  public static InventoryEvent createInventoryEvent(String eventId, InventoryEventType type,
                                                    InventoryResourceType resourceType, boolean isConsortiumShadowCopy) {
    var sourcePrefix = isConsortiumShadowCopy ? "CONSORTIUM-" : "";
    return InventoryEvent.builder()
      .entityId(UUID.randomUUID().toString())
      .eventId(eventId)
      .tenant(TENANT_ID)
      .type(type)
      .resourceType(resourceType)
      .eventTs(System.currentTimeMillis())
      .newValue(Map.of("key", "newValue", "source", sourcePrefix + "FOLIO"))
      .oldValue(Map.of("key", "oldValue"))
      .build();
  }

  public static SourceRecordDomainEvent createSourceRecordDomainEvent(SourceRecordDomainEventType eventType, List<Map<String, Object>> oldFields, List<Map<String, Object>> newFields) {
    Record oldRecord = null;
    Record newRecord = null;
    Map<String, Object> metadata = SourceRecordDomainEventType.SOURCE_RECORD_CREATED.equals(eventType) ? Map.of("createdByUserId", USER_ID) : Map.of("updatedByUserId", USER_ID);
    if (oldFields != null && !oldFields.isEmpty()) {
      oldRecord = new Record(SOURCE_RECORD_ID, Map.of("content", Map.of("fields", oldFields)), metadata);
    }
    if (newFields != null && !newFields.isEmpty()) {
      newRecord = new Record(SOURCE_RECORD_ID, Map.of("content", Map.of("fields", newFields)), metadata);
    }

    return new SourceRecordDomainEvent(
      UUID.randomUUID().toString(),
      SourceRecordType.MARC_BIB,
      new EventMetadata("module", TENANT_ID, LocalDateTime.now()),
      new MarcEventPayload(
        newRecord,
        oldRecord
      ), eventType
    );
  }

  public static SourceRecordDomainEvent createSourceRecordDomainEvent() {
    return new SourceRecordDomainEvent(
      UUID.randomUUID().toString(),
      SourceRecordType.MARC_BIB,
      new EventMetadata("module", TENANT_ID, LocalDateTime.now()),
      new MarcEventPayload(
        new org.folio.util.marc.Record(SOURCE_RECORD_ID, Map.of(
          "content", Map.of(
            "leader", LEADER_NEW,
            "fields", List.of(
              Map.of(FIELD_001, VALUE_001),
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_NEW))))
            )
          )
        ), Map.of("createdByUserId", USER_ID)),
        null
      ), SourceRecordDomainEventType.SOURCE_RECORD_CREATED
    );
  }

  public static SourceRecordDomainEvent updateSourceRecordDomainEvent() {
    return new SourceRecordDomainEvent(
      UUID.randomUUID().toString(),
      SourceRecordType.MARC_BIB,
      new EventMetadata("module", TENANT_ID, LocalDateTime.now()),
      new MarcEventPayload(
        new org.folio.util.marc.Record(SOURCE_RECORD_ID, Map.of(
          "content", Map.of(
            "leader", LEADER_NEW,
            "fields", List.of(
              Map.of(FIELD_001, VALUE_001),
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_NEW))))
            )
          )
        ), Map.of("updatedByUserId", USER_ID)),
        new org.folio.util.marc.Record(SOURCE_RECORD_ID, Map.of(
          "content", Map.of(
            "leader", LEADER_OLD,
            "fields", List.of(
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_OLD))))
            )
          )
        ), Map.of("updatedByUserId", USER_ID))
      ), SourceRecordDomainEventType.SOURCE_RECORD_UPDATED
    );
  }

  public static SourceRecordDomainEvent sourceRecordDomainEventWithNoDiff() {
    var event = updateSourceRecordDomainEvent();
    var newRecord = event.getEventPayload().getNewRecord();
    event.getEventPayload().setOld(newRecord);
    return event;
  }

  public static MarcAuditEntity createMarcAuditEntity() {
    return new MarcAuditEntity(
      UUID.randomUUID().toString(),
      LocalDateTime.now(),
      UUID.randomUUID().toString(),
      "origin",
      "action",
      UUID.randomUUID().toString(),
      null
    );
  }
}
