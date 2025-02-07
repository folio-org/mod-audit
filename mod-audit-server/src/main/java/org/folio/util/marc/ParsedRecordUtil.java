package org.folio.util.marc;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.exception.ValidationException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for mapping and transforming parsed MARC records into entities suitable
 * for audit and logging purposes. This class provides methods to process and compare parsed
 * MARC records and extract relevant data for creating audit entities.
 * <p>
 * The {@code ParsedRecordUtil} handles operations such as:
 * - Mapping a source record domain event to a MarcAuditEntity.
 * - Extracting user IDs from metadata.
 * - Flattening complex parsed record structures into more manageable formats.
 * - Calculating differences between old and new parsed records for auditing purposes.
 * - Formatting and restructuring MARC field data.
 * </p>
 * <p>
 * This class is intended to function as a utility and should not be instantiated directly.
 */
@UtilityClass
public class ParsedRecordUtil {
  private static final String LDR = "LDR";
  private static final String FIELD_005 = "005";
  private static final String SUBFIELDS_KEY = "subfields";
  private static final String FIELD_KEY = "field";
  private static final String FIELDS_KEY = "fields";
  private static final String LEADER_KEY = "leader";
  private static final String VALUE_KEY = "value";
  private static final String CREATED_BY = "createdByUserId";
  private static final String UPDATED_BY = "updatedByUserId";
  private static final String OLD_VALUE_KEY = "oldValue";
  private static final String NEW_VALUE_KEY = "newValue";

  /**
   * Maps a {@link SourceRecordDomainEvent} to a {@link MarcAuditEntity}.
   *
   * @param event the source record domain event containing the details for creating the MarcAuditEntity;
   *              it includes event metadata, event type, payload with new and old records, and event ID.
   * @return a {@link MarcAuditEntity} instance containing data inferred from the given event, such as event ID,
   * event date, record ID, origin, action, user ID, record type, and any differences.
   */
  public static MarcAuditEntity mapToEntity(SourceRecordDomainEvent event) {
    RecordData data;
    Map<String, Object> difference;
    var oldRecord = event.getEventPayload().getOld();
    var newRecord = event.getEventPayload().getNewRecord();

    switch (event.getEventType()) {
      case SOURCE_RECORD_CREATED -> {
        data = extractRecordData(newRecord, CREATED_BY);
        difference = getDifference(newRecord.getParsedRecord(), event.getEventType());
      }
      case SOURCE_RECORD_UPDATED -> {
        data = extractRecordData(newRecord, UPDATED_BY);
        difference = calculateDifferences(oldRecord.getParsedRecord(), newRecord.getParsedRecord());
      }
      case SOURCE_RECORD_DELETED -> {
        data = extractRecordData(oldRecord, UPDATED_BY);
        difference = getDifference(oldRecord.getParsedRecord(), event.getEventType());
      }
      default -> throw new ValidationException("Unsupported event type: " + event.getEventType());
    }

    return new MarcAuditEntity(
      event.getEventId(),
      event.getEventMetadata().getEventDate(),
      data.recordId,
      event.getEventMetadata().getPublishedBy(),
      event.getEventType().getValue(),
      data.userId,
      data.recordType,
      difference
    );
  }

  private static RecordData extractRecordData(Record value, String userKey) {
    return new RecordData(
      value.getId(),
      getUserIdFromMetadata(value.getMetadata(), userKey),
      value.getRecordType()
    );
  }

  private static String getUserIdFromMetadata(Map<String, Object> metadata, String key) {
    return metadata.get(key).toString();
  }

  private static Map<String, Object> calculateDifferences(Map<String, Object> oldParsedRecord, Map<String, Object> newParsedRecord) {
    var oldFields = flattenFields(oldParsedRecord);
    var newFields = flattenFields(newParsedRecord);
    return compareParsedRecords(oldFields, newFields);
  }

  private static Map<String, Object> getDifference(Map<String, Object> parsedRecord, SourceRecordDomainEventType type) {
    List<Map<String, Object>> fields = new ArrayList<>();
    flattenFields(parsedRecord).forEach((key, value) ->
      fields.add(Map.of(FIELD_KEY, key, VALUE_KEY, value))
    );
    return type.equals(SourceRecordDomainEventType.SOURCE_RECORD_CREATED)
      ? toMap(fields, null, null)
      : toMap(null, fields, null);
  }

  /**
   * Flattens the fields in the given parsed record map to a single-level map structure.
   * The method processes the "content" key in the input map, extracts the field data,
   * and formats it based on specified rules, combining fields with the same tag into lists when necessary.
   *
   * @param input a map representing a parsed record, expected to contain a "content" key
   *              with relevant field data and metadata.
   * @return a map where field tags are the keys and their corresponding values
   * are formatted strings or lists of strings.
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> flattenFields(Map<String, Object> input) {
    var content = getContent(input);
    var fields = (List<Map<String, Object>>) content.get(FIELDS_KEY);
    var result = new HashMap<String, Object>();
    if (content.containsKey(LEADER_KEY)) {
      result.put(LDR, content.get(LEADER_KEY));
    }
    if (!content.containsKey(FIELDS_KEY)) {
      return result;
    }
    fields.forEach(fieldObj -> fieldObj.forEach((tag, value) -> {
      var formattedValue = formatField(value);
      result.merge(tag, formattedValue, (existing, newValue) -> {
        if (existing instanceof List) {
          ((List<Object>) existing).add(newValue);
          return existing;
        }
        return new ArrayList<>(Arrays.asList(existing, newValue));
      });
    }));
    return result;
  }

  /**
   * Formats the given field value into a standardized string representation.
   * If the input value is a map containing subfields, it creates a formatted string
   * combining indicators and subfields. Otherwise, it converts the value to a string.
   *
   * @param value the field value to format, which can be an object or a map with subfields
   *              and indicators ("ind1", "ind2").
   * @return a formatted string representation of the field.
   */
  @SuppressWarnings("unchecked")
  private static String formatField(Object value) {
    if (value instanceof Map<?, ?> map && map.containsKey(SUBFIELDS_KEY)) {
      var fieldData = (Map<String, Object>) map;
      var ind1 = (String) fieldData.getOrDefault("ind1", " ");
      var ind2 = (String) fieldData.getOrDefault("ind2", " ");
      var subfields = (List<Map<String, Object>>) fieldData.get(SUBFIELDS_KEY);
      var subfieldsString = subfields.stream()
        .flatMap(subObj -> subObj.entrySet().stream()
          .map(e -> "$" + e.getKey() + e.getValue()))
        .collect(Collectors.joining());
      return ind1 + ind2 + subfieldsString;
    }
    return value.toString();
  }

  /**
   * Compares two maps representing parsed records and identifies added, modified,
   * and removed entries. It determines the differences by comparing the keys
   * and values of the given maps.
   *
   * @param oldMap the original map representing the state of a parsed record before changes
   * @param newMap the updated map representing the state of a parsed record after changes
   * @return a map containing three keys ("added", "removed", "modified"), each mapped
   * to a list of maps. The "added" key contains entries present only in the new map,
   * the "removed" key contains entries present only in the old map,
   * and the "modified" key contains entries with mismatched values between the maps.
   */
  private static Map<String, Object> compareParsedRecords(Map<String, Object> oldMap, Map<String, Object> newMap) {
    List<Map<String, Object>> added = new ArrayList<>();
    List<Map<String, Object>> modified = new ArrayList<>();
    List<Map<String, Object>> removed = new ArrayList<>();

    newMap.forEach((key, newValue) -> {
      if (!oldMap.containsKey(key)) {
        added.add(Map.of(FIELD_KEY, key, VALUE_KEY, newValue));
      }
    });

    oldMap.forEach((key, oldValue) -> {
      if (!newMap.containsKey(key)) {
        removed.add(Map.of(FIELD_KEY, key, VALUE_KEY, oldValue));
      }
    });

    newMap.forEach((key, newValue) -> {
      if (!FIELD_005.equals(key) && oldMap.containsKey(key)) {
        var oldValue = oldMap.get(key);
        if (!Objects.equals(oldValue, newValue)) {
          modified.add(Map.of(
            FIELD_KEY, key,
            OLD_VALUE_KEY, oldValue,
            NEW_VALUE_KEY, newValue
          ));
        }
      }
    });

    return toMap(added, removed, modified);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getContent(Map<String, Object> parsedRecord) {
    var contentObj = parsedRecord.get("content");
    Map<String, Object> content;
    if (contentObj instanceof String contentStr) {
      content = new JsonObject(contentStr).getMap();
    } else if (contentObj instanceof Map<?, ?> contentMap) {
      content = (Map<String, Object>) contentMap;
    } else {
      return Collections.emptyMap();
    }
    return content;
  }

  private static Map<String, Object> toMap(Collection<?> added, Collection<?> removed, Collection<?> modified) {
    Map<String, Object> result = new HashMap<>();
    addIfNotEmpty(result, "added", added);
    addIfNotEmpty(result, "removed", removed);
    addIfNotEmpty(result, "modified", modified);
    return result;
  }

  private static void addIfNotEmpty(Map<String, Object> map, String key, Collection<?> collection) {
    if (collection != null && !collection.isEmpty()) {
      map.put(key, collection);
    }
  }

  private record RecordData(String recordId, String userId, SourceRecordType recordType) {
  }

}
