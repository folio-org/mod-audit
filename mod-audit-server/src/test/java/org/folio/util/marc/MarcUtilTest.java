package org.folio.util.marc;

import org.folio.dao.marc.MarcAuditEntity;
import org.folio.domain.diff.ChangeType;
import org.folio.utils.EntityUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.utils.EntityUtils.FIELD_001;
import static org.folio.utils.EntityUtils.VALUE_001;
import static org.folio.utils.EntityUtils.updateSourceRecordDomainEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarcUtilTest {
  private static final String FIELD_020 = "020";
  private static final String FIELD_999 = "999";

  @Test
  void testMapToEntity_SourceRecordCreated_MarcBib() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    assertEquals(3, entity.diff().getFieldChanges().size());
  }

  @Test
  void testMapToEntity_SourceRecordUpdated_MarcBib() {
    var event = updateSourceRecordDomainEvent();
    var prefix = "10 $a ";
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    var changeRecord = entity.diff();
    assertNotNull(changeRecord);

    var fieldChanges = changeRecord.getFieldChanges();
    var collectionChanges = changeRecord.getCollectionChanges();

    // Assert there are exactly three field changes
    assertEquals(3, fieldChanges.size());
    assertTrue(collectionChanges.isEmpty());

    // Find the title field change
    var titleFieldChange = fieldChanges.stream()
      .filter(change -> change.getFieldName().equals(EntityUtils.FIELD_245))
      .findFirst()
      .orElseThrow();

    assertEquals(ChangeType.MODIFIED, titleFieldChange.getChangeType());
    assertEquals(prefix + EntityUtils.TITLE_OLD, titleFieldChange.getOldValue());
    assertEquals(prefix + EntityUtils.TITLE_NEW, titleFieldChange.getNewValue());
  }


  @Test
  void testMapToEntity_SourceRecordUnchanged() {
    var event = EntityUtils.sourceRecordDomainEventWithNoDiff();

    var entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);

    var diff = entity.diff();
    assertNotNull(diff, "Diff should not be null even if there are no changes.");
    assertTrue(diff.getFieldChanges().isEmpty(), "Field changes should be empty for an unchanged record.");
    assertTrue(diff.getCollectionChanges().isEmpty(), "Collection changes should be empty for an unchanged record.");
  }

  @Test
  void mapToEntity_UpdatesMappedCorrectly() {
    // Arrange
    var event = updateSourceRecordDomainEvent();

    // Act
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    // Assert
    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    var diff = entity.diff();
    assertNotNull(diff);

    var fieldChanges = diff.getFieldChanges();
    var collectionChanges = diff.getCollectionChanges();

    assertEquals(3, fieldChanges.size()); // Example count, adjust if needed
    assertTrue(collectionChanges.isEmpty());

    // Verify added field (001)
    var addedField = fieldChanges.stream()
      .filter(change -> change.getFieldName().equals(FIELD_001) && change.getChangeType() == ChangeType.ADDED)
      .findFirst()
      .orElseThrow();

    assertNull(addedField.getOldValue());
    assertEquals(VALUE_001, addedField.getNewValue());

    // Verify modified field (LDR)
    var modifiedLeader = fieldChanges.stream()
      .filter(change -> change.getFieldName().equals("LDR") && change.getChangeType() == ChangeType.MODIFIED)
      .findFirst()
      .orElseThrow();

    assertEquals(EntityUtils.LEADER_OLD, modifiedLeader.getOldValue());
    assertEquals(EntityUtils.LEADER_NEW, modifiedLeader.getNewValue());
  }


  @Test
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

    var diff = entity.diff();
    assertNotNull(diff);

    var fieldChanges = diff.getFieldChanges();
    var collectionChanges = diff.getCollectionChanges();

    assertEquals(2, fieldChanges.size());
    assertTrue(collectionChanges.isEmpty());

    // Check LDR is removed
    var removedLeader = fieldChanges.stream()
      .filter(change -> change.getFieldName().equals("LDR") && change.getChangeType() == ChangeType.REMOVED)
      .findFirst()
      .orElseThrow();

    assertEquals(EntityUtils.LEADER_OLD, removedLeader.getOldValue());
    assertNull(removedLeader.getNewValue());
  }


  @Test
  void testMapToEntity_AddNewRepeatableField() {
    var oldValue = "020_1";
    var newValue = "020_2";
    Map<String, Object> oldFields = Map.of(FIELD_020, oldValue);
    List<Map<String, Object>> newFields = List.of(
      Map.of(FIELD_020, oldValue),
      Map.of(FIELD_020, newValue)
    );

    var event = EntityUtils.createSourceRecordDomainEvent(
      SourceRecordDomainEventType.SOURCE_RECORD_UPDATED,
      List.of(oldFields),
      newFields
    );

    var diff = MarcUtil.mapToEntity(event).diff();

    assertNotNull(diff);
    assertTrue(diff.getFieldChanges().isEmpty());

    var collectionChanges = diff.getCollectionChanges();
    assertEquals(1, collectionChanges.size());

    var collectionChange = collectionChanges.getFirst();
    assertEquals(FIELD_020, collectionChange.getCollectionName());
    assertEquals(1, collectionChange.getItemChanges().size());

    var itemChange = collectionChange.getItemChanges().getFirst();
    assertEquals(ChangeType.ADDED, itemChange.getChangeType());
    assertNull(itemChange.getOldValue());
    assertEquals(newValue, itemChange.getNewValue());
  }


  @Test
  void testMapToEntity_RemoveRepeatableField() {
    var toRemoveValue = "020_to_remove";
    List<Map<String, Object>> oldFields = List.of(
      Map.of(FIELD_020, "020_1"),
      Map.of(FIELD_020, toRemoveValue)
    );
    Map<String, Object> newFields = Map.of(FIELD_020, "020_1");

    var event = EntityUtils.createSourceRecordDomainEvent(
      SourceRecordDomainEventType.SOURCE_RECORD_UPDATED,
      oldFields,
      List.of(newFields)
    );

    var diff = MarcUtil.mapToEntity(event).diff();

    assertNotNull(diff);
    assertTrue(diff.getFieldChanges().isEmpty());

    var collectionChanges = diff.getCollectionChanges();
    assertEquals(1, collectionChanges.size());

    var collectionChange = collectionChanges.getFirst();
    assertEquals(FIELD_020, collectionChange.getCollectionName());
    assertEquals(1, collectionChange.getItemChanges().size());

    var itemChange = collectionChange.getItemChanges().getFirst();
    assertEquals(ChangeType.REMOVED, itemChange.getChangeType());
    assertEquals(toRemoveValue, itemChange.getOldValue());
    assertNull(itemChange.getNewValue());
  }

  @Test
  void testMapToEntity_UpdateRepeatableField() {
    // Arrange
    List<Map<String, Object>> oldFields = List.of(
      Map.of(FIELD_020, "020_1"),
      Map.of(FIELD_020, "020_2")
    );
    List<Map<String, Object>> newFields = List.of(
      Map.of(FIELD_020, "020_1"),
      Map.of(FIELD_020, "020_3")
    );

    var event = EntityUtils.createSourceRecordDomainEvent(
      SourceRecordDomainEventType.SOURCE_RECORD_UPDATED,
      oldFields,
      newFields
    );

    // Act
    var diff = MarcUtil.mapToEntity(event).diff();

    // Assert
    assertNotNull(diff);
    assertTrue(diff.getFieldChanges().isEmpty());

    var collectionChanges = diff.getCollectionChanges();
    assertEquals(1, collectionChanges.size());

    var collectionChange = collectionChanges.getFirst();
    assertEquals(FIELD_020, collectionChange.getCollectionName());

    var itemChanges = collectionChange.getItemChanges();
    assertEquals(2, itemChanges.size());

    var removedItem = itemChanges.stream()
      .filter(item -> item.getChangeType() == ChangeType.REMOVED)
      .findFirst()
      .orElseThrow();
    assertEquals("020_2", removedItem.getOldValue());
    assertNull(removedItem.getNewValue());

    var addedItem = itemChanges.stream()
      .filter(item -> item.getChangeType() == ChangeType.ADDED)
      .findFirst()
      .orElseThrow();
    assertNull(addedItem.getOldValue());
    assertEquals("020_3", addedItem.getNewValue());
  }

  @Test
  void testMapToEntity_IgnoreUpdateIn999FF() {
    // Arrange
    List<Map<String, Object>> oldFields = List.of(
      Map.of(FIELD_999, "ff$s%s$i%s".formatted(UUID.randomUUID().toString(), UUID.randomUUID().toString())),
      Map.of(FIELD_999, "999_to_update"),
      Map.of(FIELD_999, "999_to_remove"),
      Map.of(FIELD_999, "999_to_stay")
    );
    List<Map<String, Object>> newFields = List.of(
      Map.of(FIELD_999, "ff$s%s$i%s".formatted(UUID.randomUUID().toString(), UUID.randomUUID().toString())),
      Map.of(FIELD_999, "999_updated"),
      Map.of(FIELD_999, "999_to_stay")
    );

    var event = EntityUtils.createSourceRecordDomainEvent(
      SourceRecordDomainEventType.SOURCE_RECORD_UPDATED,
      oldFields,
      newFields
    );

    // Act
    var diff = MarcUtil.mapToEntity(event).diff();

    // Assert
    assertNotNull(diff);
    assertTrue(diff.getFieldChanges().isEmpty());

    var collectionChanges = diff.getCollectionChanges();
    assertEquals(1, collectionChanges.size());

    var collectionChange = collectionChanges.getFirst();
    assertEquals(FIELD_999, collectionChange.getCollectionName());

    var itemChanges = collectionChange.getItemChanges();
    assertEquals(3, itemChanges.size());

    var removedItems = itemChanges.stream()
      .filter(item -> item.getChangeType() == ChangeType.REMOVED)
      .toList();
    assertFalse(removedItems.stream().anyMatch(item -> item.getOldValue().toString().startsWith("ff")));
    assertEquals(2, removedItems.size());

    var addedItem = itemChanges.stream()
      .filter(item -> item.getChangeType() == ChangeType.ADDED)
      .toList();
    assertEquals(1, addedItem.size());
    assertFalse(addedItem.getFirst().getNewValue().toString().startsWith("ff"));
  }
}
