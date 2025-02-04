package org.folio.util.marc;

import org.folio.dao.marc.MarcAuditEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsedRecordUtilTest {

  private static final String EVENT_ID = UUID.randomUUID().toString();
  private static final String USER_ID = UUID.randomUUID().toString();
  private static final String MODULE_NAME = "mod-source-record-storage";
  private static final String TENANT_ID = "test-tenant";
  private static final String FIELD_KEY = "field";
  private static final String OLD_VALUE_KEY = "oldValue";
  private static final String NEW_VALUE_KEY = "newValue";

  private static final String LEADER_OLD = "old-leader";
  private static final String LEADER_NEW = "updated-leader";
  private static final String LEADER_UNCHANGED = "00123nam 2200037 a 4500";

  private static final String FIELD_001 = "001";
  private static final String FIELD_245 = "245";
  private static final String TITLE_OLD = "Old Title";
  private static final String TITLE_NEW = "Updated Title";
  private static final String TITLE_UNCHANGED = "Same Title";

  @Test
  void testMapToEntity_SourceRecordCreated_MarcBib() {
    var event = createEvent();

    MarcAuditEntity entity = ParsedRecordUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(EVENT_ID, entity.eventId());
    assertEquals(USER_ID, entity.userId());
    assertEquals(SourceRecordType.MARC_BIB, entity.recordType());

    assertTrue(entity.diff().containsKey("added"));
    assertEquals(3, ((List<?>) entity.diff().get("added")).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_SourceRecordUpdated_MarcBib() {
    var event = updateEvent();
    var prefix = "10$a";
    MarcAuditEntity entity = ParsedRecordUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(EVENT_ID, entity.eventId());
    assertEquals(USER_ID, entity.userId());
    assertEquals(SourceRecordType.MARC_BIB, entity.recordType());

    assertTrue(entity.diff().containsKey("modified"));
    assertEquals(2, ((List<?>) entity.diff().get("modified")).size());

    List<Map<String, Object>> modifiedFields = (List<Map<String, Object>>) entity.diff().get("modified");
    Map<String, Object> titleFieldDiff = modifiedFields.stream()
      .filter(change -> change.get(FIELD_KEY).equals(FIELD_245))
      .findFirst().orElseThrow();

    assertEquals(prefix + TITLE_OLD, titleFieldDiff.get(OLD_VALUE_KEY));
    assertEquals(prefix + TITLE_NEW, titleFieldDiff.get(NEW_VALUE_KEY));
  }

  @Test
  void testMapToEntity_SourceRecordUnchanged() {
    var event = updateEventWithoutChanges();

    MarcAuditEntity entity = ParsedRecordUtil.mapToEntity(event);

    assertNotNull(entity);
    assertTrue(entity.diff().isEmpty(), "Diff should be empty for an unchanged record.");
  }

  @Test
  @SuppressWarnings("unchecked")
  void mapToEntity_UpdatesMappedCorrectly() {
    // Arrange
    var event = updateEvent();

    // Act
    MarcAuditEntity entity = ParsedRecordUtil.mapToEntity(event);

    // Assert
    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(USER_ID, entity.userId());
    assertEquals(SourceRecordType.MARC_BIB, entity.recordType());

    // Verify differences calculated for update
    Map<String, Object> diff = entity.diff();
    assertTrue(diff.containsKey("modified"));
    assertFalse(diff.containsKey("added"));
    assertFalse(diff.containsKey("removed"));

    var modifiedFields = (List<Map<String, Object>>) diff.get("modified");
    assertEquals(2, modifiedFields.size());
    assertEquals("LDR", modifiedFields.get(1).get("field"));
    assertEquals(LEADER_OLD, modifiedFields.get(1).get("oldValue"));
    assertEquals(LEADER_NEW, modifiedFields.get(1).get("newValue"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void mapToEntity_DefaultCaseHandled() {
    // Arrange
    var event = deleteEvent();

    // Act
    MarcAuditEntity entity = ParsedRecordUtil.mapToEntity(event);

    // Assert
    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(USER_ID, entity.userId());
    assertEquals(SourceRecordType.MARC_AUTHORITY, entity.recordType());

    // Verify differences calculated for deletion
    Map<String, Object> diff = entity.diff();
    assertTrue(diff.containsKey("removed"));
    assertFalse(diff.containsKey("added"));
    assertFalse(diff.containsKey("modified"));

    var removedFields = (List<Map<String, Object>>) diff.get("removed");
    assertEquals(3, removedFields.size());
    assertEquals("LDR", removedFields.get(2).get("field"));
    assertEquals(LEADER_OLD, removedFields.get(2).get("value"));
  }


  private SourceRecordDomainEvent createEvent() {
    return new SourceRecordDomainEvent(
      EVENT_ID,
      SourceRecordDomainEventType.SOURCE_RECORD_CREATED,
      new EventMetadata(MODULE_NAME, TENANT_ID, LocalDateTime.now()),
      new EventPayload(
        new Record(EVENT_ID, SourceRecordType.MARC_BIB, Map.of(
          "content", Map.of(
            "leader", LEADER_UNCHANGED,
            "fields", List.of(
              Map.of(FIELD_001, EVENT_ID),
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_UNCHANGED))))
            )
          )
        ), Map.of("createdByUserId", USER_ID)),
        null
      ),
      EVENT_ID
    );
  }

  private SourceRecordDomainEvent updateEvent() {
    return new SourceRecordDomainEvent(
      EVENT_ID,
      SourceRecordDomainEventType.SOURCE_RECORD_UPDATED,
      new EventMetadata(MODULE_NAME, TENANT_ID, LocalDateTime.now()),
      new EventPayload(
        new Record(EVENT_ID, SourceRecordType.MARC_BIB, Map.of(
          "content", Map.of(
            "leader", LEADER_NEW,
            "fields", List.of(
              Map.of(FIELD_001, EVENT_ID),
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_NEW))))
            )
          )
        ), Map.of("updatedByUserId", USER_ID)),
        new Record(EVENT_ID, SourceRecordType.MARC_BIB, Map.of(
          "content", Map.of(
            "leader", LEADER_OLD,
            "fields", List.of(
              Map.of(FIELD_001, EVENT_ID),
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_OLD))))
            )
          )
        ), Map.of("updatedByUserId", USER_ID))
      ),
      EVENT_ID
    );
  }

  private SourceRecordDomainEvent deleteEvent() {
    return new SourceRecordDomainEvent(
      "event-003",
      SourceRecordDomainEventType.SOURCE_RECORD_DELETED,
      new EventMetadata(MODULE_NAME, TENANT_ID, LocalDateTime.now()),
      new EventPayload(
        null,
        new Record(EVENT_ID, SourceRecordType.MARC_AUTHORITY, Map.of(
          "content", Map.of(
            "leader", LEADER_OLD,
            "fields", List.of(
              Map.of(FIELD_001, "record-003"),
              Map.of(FIELD_245, Map.of(
                "ind1", "1",
                "ind2", "0",
                "subfields", List.of(Map.of("a", "Deleted Title"))
              ))
            )
          )
        ), Map.of("updatedByUserId", USER_ID))
      ),
      "record-003"
    );
  }

  private SourceRecordDomainEvent updateEventWithoutChanges() {
    return new SourceRecordDomainEvent(
      EVENT_ID,
      SourceRecordDomainEventType.SOURCE_RECORD_UPDATED,
      new EventMetadata(MODULE_NAME, TENANT_ID, LocalDateTime.now()),
      new EventPayload(
        new Record(EVENT_ID, SourceRecordType.MARC_BIB, Map.of(
          "content", Map.of(
            "leader", LEADER_UNCHANGED,
            "fields", List.of(
              Map.of(FIELD_001, EVENT_ID),
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_UNCHANGED))))
            )
          )
        ), Map.of("updatedByUserId", USER_ID)),
        new Record(EVENT_ID, SourceRecordType.MARC_BIB, Map.of(
          "content", Map.of(
            "leader", LEADER_UNCHANGED,
            "fields", List.of(
              Map.of(FIELD_001, EVENT_ID),
              Map.of(FIELD_245, Map.of("ind1", "1", "ind2", "0", "subfields", List.of(Map.of("a", TITLE_UNCHANGED))))
            )
          )
        ), Map.of("updatedByUserId", USER_ID))
      ),
      EVENT_ID
    );
  }
}
