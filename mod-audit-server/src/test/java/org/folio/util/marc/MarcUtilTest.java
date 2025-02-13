package org.folio.util.marc;

import org.folio.dao.marc.MarcAuditEntity;
import org.folio.utils.EntityUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarcUtilTest {
  private static final String FIELD_KEY = "field";
  private static final String OLD_VALUE_KEY = "oldValue";
  private static final String NEW_VALUE_KEY = "newValue";

  @Test
  void testMapToEntity_SourceRecordCreated_MarcBib() {
    var event = EntityUtils.createSourceRecordDomainEvent();

    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    assertTrue(entity.diff().containsKey("added"));
    assertEquals(3, ((List<?>) entity.diff().get("added")).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_SourceRecordUpdated_MarcBib() {
    var event = EntityUtils.updateSourceRecordDomainEvent();
    var prefix = "10$a";
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    assertTrue(entity.diff().containsKey("modified"));
    assertEquals(2, ((List<?>) entity.diff().get("modified")).size());

    List<Map<String, Object>> modifiedFields = (List<Map<String, Object>>) entity.diff().get("modified");
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
    var event = EntityUtils.updateSourceRecordDomainEvent();

    // Act
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    // Assert
    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    // Verify differences calculated for update
    Map<String, Object> diff = entity.diff();
    assertTrue(diff.containsKey("modified"));
    assertFalse(diff.containsKey("added"));
    assertFalse(diff.containsKey("removed"));

    var modifiedFields = (List<Map<String, Object>>) diff.get("modified");
    assertEquals(2, modifiedFields.size());
    assertEquals("LDR", modifiedFields.get(1).get("field"));
    assertEquals(EntityUtils.LEADER_OLD, modifiedFields.get(1).get("oldValue"));
    assertEquals(EntityUtils.LEADER_NEW, modifiedFields.get(1).get("newValue"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testMapToEntity_DeleteRecordUpdated_MarcBib() {
    // Arrange
    var event = EntityUtils.deleteSourceRecordDomainEvent();

    // Act
    MarcAuditEntity entity = MarcUtil.mapToEntity(event);

    // Assert
    assertNotNull(entity);
    assertEquals(event.getEventId(), entity.eventId());
    assertEquals(EntityUtils.USER_ID, entity.userId());

    // Verify differences calculated for deletion
    Map<String, Object> diff = entity.diff();
    assertTrue(diff.containsKey("removed"));
    assertFalse(diff.containsKey("added"));
    assertFalse(diff.containsKey("modified"));

    var removedFields = (List<Map<String, Object>>) diff.get("removed");
    assertEquals(3, removedFields.size());
    assertEquals("LDR", removedFields.get(2).get("field"));
    assertEquals(EntityUtils.LEADER_OLD, removedFields.get(2).get("value"));
  }
}
