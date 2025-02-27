package org.folio.util.marc;

import io.vertx.core.json.Json;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.utils.EntityUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.folio.utils.EntityUtils.FIELD_001;
import static org.folio.utils.EntityUtils.VALUE_001;
import static org.folio.utils.EntityUtils.updateSourceRecordDomainEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarcUtilTest {
  private static final String FIELD_KEY = "field";
  private static final String OLD_VALUE_KEY = "oldValue";
  private static final String NEW_VALUE_KEY = "newValue";
  private static final String ADDED_KEY = "added";
  private static final String REMOVED_KEY = "removed";
  private static final String MODIFIED_KEY = "modified";
  private static final String FIELD_020 = "020";

  @Test
  void testMapToEntity_SourceRecordCreated_MarcBib() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    assertTrue(entity.diff().containsKey(ADDED_KEY));
    assertEquals(3, ((List<?>) entity.diff().get(ADDED_KEY)).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_SourceRecordUpdated_MarcBib() {
    var event = updateSourceRecordDomainEvent();
    var prefix = "10$a";
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    assertTrue(entity.diff().containsKey(MODIFIED_KEY));
    assertEquals(2, ((List<?>) entity.diff().get(MODIFIED_KEY)).size());

    List<Map<String, Object>> modifiedFields = (List<Map<String, Object>>) entity.diff().get(MODIFIED_KEY);
    Map<String, Object> titleFieldDiff = modifiedFields.stream()
      .filter(change -> change.get(FIELD_KEY).equals(EntityUtils.FIELD_245))
      .findFirst().orElseThrow();

    assertEquals(prefix + EntityUtils.TITLE_OLD, titleFieldDiff.get(OLD_VALUE_KEY));
    assertEquals(prefix + EntityUtils.TITLE_NEW, titleFieldDiff.get(NEW_VALUE_KEY));
  }

  @Test
  void testMapToEntity_SourceRecordUnchanged() {
    var event = EntityUtils.sourceRecordDomainEventWithNoDiff();

    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);
    assertTrue(entity.diff().isEmpty(), "Diff should be empty for an unchanged record.");
  }

  @Test
  @SuppressWarnings("unchecked")
  void mapToEntity_UpdatesMappedCorrectly() {
    // Arrange
    var event = updateSourceRecordDomainEvent();

    // Act
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    // Assert
    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    // Verify differences calculated for update
    Map<String, Object> diff = entity.diff();
    assertTrue(diff.containsKey(MODIFIED_KEY));
    assertTrue(diff.containsKey(ADDED_KEY));
    assertFalse(diff.containsKey(REMOVED_KEY));

    var modifiedFields = (List<Map<String, Object>>) diff.get(MODIFIED_KEY);
    var addedFields = (List<Map<String, Object>>) diff.get(ADDED_KEY);
    assertEquals(2, modifiedFields.size());
    assertEquals(1, addedFields.size());
    assertEquals("LDR", modifiedFields.get(1).get(FIELD_KEY));
    assertEquals(FIELD_001, addedFields.get(0).get(FIELD_KEY));
    assertEquals(VALUE_001, addedFields.get(0).get(NEW_VALUE_KEY));
    assertEquals(EntityUtils.LEADER_OLD, modifiedFields.get(1).get(OLD_VALUE_KEY));
    assertEquals(EntityUtils.LEADER_NEW, modifiedFields.get(1).get(NEW_VALUE_KEY));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_DeleteRecordUpdated_MarcBib() {
    // Arrange
    var event = updateSourceRecordDomainEvent();
    event.getEventPayload().setNewRecord(null);
    event.setEventType(SourceRecordDomainEventType.SOURCE_RECORD_DELETED);

    // Act
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    // Assert
    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    // Verify differences calculated for deletion
    Map<String, Object> diff = entity.diff();
    assertTrue(diff.containsKey(REMOVED_KEY));
    assertFalse(diff.containsKey(ADDED_KEY));
    assertFalse(diff.containsKey(MODIFIED_KEY));

    var removedFields = (List<Map<String, Object>>) diff.get(REMOVED_KEY);
    assertEquals(2, removedFields.size());
    assertEquals("LDR", removedFields.get(1).get(FIELD_KEY));
    assertEquals(EntityUtils.LEADER_OLD, removedFields.get(1).get("oldValue"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_AddNewRepeatableField() {
    var oldValue = "020_1";
    var newValue = "020_2";
    Map<String, Object> oldFields = Map.of(FIELD_020, oldValue);
    List<Map<String, Object>> newFields = List.of(Map.of(FIELD_020, oldValue), Map.of(FIELD_020, newValue));
    var event = EntityUtils.createSourceRecordDomainEvent(SourceRecordDomainEventType.SOURCE_RECORD_UPDATED, List.of(oldFields), newFields);
    var diff = MarcUtil.mapToEntity(event).diff();

    System.out.println(Json.encode(diff));

    assertTrue(diff.containsKey(ADDED_KEY));
    assertFalse(diff.containsKey(MODIFIED_KEY));
    assertFalse(diff.containsKey(REMOVED_KEY));

    var added = (List<Map<String, Object>>) diff.get(ADDED_KEY);
    assertEquals(1, added.size());
    assertEquals(FIELD_020, added.get(0).get(FIELD_KEY));
    assertEquals(newValue, added.get(0).get(NEW_VALUE_KEY));
    assertFalse(added.get(0).containsKey(OLD_VALUE_KEY));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_RemoveRepeatableField() {
    var toRemoveValue = "020_to_remove";
    List<Map<String, Object>> oldFields = List.of(Map.of(FIELD_020, "020_1"), Map.of(FIELD_020, toRemoveValue));
    Map<String, Object> newFields = Map.of(FIELD_020, "020_1");
    var event = EntityUtils.createSourceRecordDomainEvent(SourceRecordDomainEventType.SOURCE_RECORD_UPDATED, oldFields, List.of(newFields));
    var diff = MarcUtil.mapToEntity(event).diff();

    assertTrue(diff.containsKey(REMOVED_KEY));
    assertFalse(diff.containsKey(ADDED_KEY));
    assertFalse(diff.containsKey(MODIFIED_KEY));

    var removed = (List<Map<String, Object>>) diff.get(REMOVED_KEY);
    assertEquals(1, removed.size());
    assertEquals(FIELD_020, removed.get(0).get(FIELD_KEY));
    assertEquals(toRemoveValue, removed.get(0).get(OLD_VALUE_KEY));
    assertFalse(removed.get(0).containsKey(NEW_VALUE_KEY));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_UpdateRepeatableField() {
    List<Map<String, Object>> oldFields = List.of(Map.of(FIELD_020, "020_1"), Map.of(FIELD_020, "020_2"));
    List<Map<String, Object>> newFields = List.of(Map.of(FIELD_020, "020_1"), Map.of(FIELD_020, "020_3"));
    var event = EntityUtils.createSourceRecordDomainEvent(SourceRecordDomainEventType.SOURCE_RECORD_UPDATED, oldFields, newFields);
    var diff = MarcUtil.mapToEntity(event).diff();

    assertTrue(diff.containsKey(MODIFIED_KEY));
    assertFalse(diff.containsKey(REMOVED_KEY));
    assertFalse(diff.containsKey(ADDED_KEY));

    var modified = (List<Map<String, Object>>) diff.get(MODIFIED_KEY);
    assertEquals(1, modified.size());
    assertEquals(FIELD_020, modified.get(0).get(FIELD_KEY));
    assertEquals(List.of("020_1", "020_3"), modified.get(0).get(NEW_VALUE_KEY));
    assertEquals(List.of("020_1", "020_2"), modified.get(0).get(OLD_VALUE_KEY));
  }
}
