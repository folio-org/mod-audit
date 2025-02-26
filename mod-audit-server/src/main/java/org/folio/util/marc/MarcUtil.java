package org.folio.util.marc;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.MarcAuditCollection;
import org.folio.rest.jaxrs.model.MarcAuditItem;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class MarcUtil {
  private static final String LDR = "LDR";
  private static final String FIELD_005 = "005";
  private static final String SUBFIELDS_KEY = "subfields";
  private static final String FIELD_KEY = "field";
  private static final String FIELDS_KEY = "fields";
  private static final String LEADER_KEY = "leader";
  private static final String CREATED_BY = "createdByUserId";
  private static final String UPDATED_BY = "updatedByUserId";
  private static final String OLD_VALUE_KEY = "oldValue";
  private static final String NEW_VALUE_KEY = "newValue";
  private static final String CREATED = "CREATED";
  private static final String UPDATED = "UPDATED";
  private static final String DELETED = "DELETED";

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
        data = extractRecordData(newRecord, CREATED_BY, CREATED);
        difference = getDifference(newRecord.getParsedRecord(), event.getEventType());
      }
      case SOURCE_RECORD_UPDATED -> {
        data = extractRecordData(newRecord, UPDATED_BY, UPDATED);
        difference = calculateDifferences(oldRecord.getParsedRecord(), newRecord.getParsedRecord());
      }
      case SOURCE_RECORD_DELETED -> {
        data = extractRecordData(oldRecord, UPDATED_BY, DELETED);
        difference = getDifference(oldRecord.getParsedRecord(), event.getEventType());
      }
      default -> throw new ValidationException("Unsupported event type: " + event.getEventType());
    }

    return new MarcAuditEntity(
      event.getEventId(),
      event.getEventMetadata().getEventDate(),
      data.recordId,
      event.getEventMetadata().getPublishedBy(),
      data.action,
      data.userId,
      difference
    );
  }

  /**
   * Maps a list of MarcAuditEntity objects to a MarcAuditCollection object.
   *
   * @param entities the list of MarcAuditEntity objects to be mapped
   * @return a MarcAuditCollection containing the mapped MarcAuditItems
   */
  public static MarcAuditCollection mapToCollection(List<MarcAuditEntity> entities) {
    var collection = new MarcAuditCollection();
    collection.setMarcAuditItems(entities.stream()
      .map(MarcUtil::mapEntityToItem)
      .toList());
    return collection;
  }

  /**
   * Maps a MarcAuditEntity to a MarcAuditItem.
   *
   * @param entity the MarcAuditEntity to be mapped
   * @return a MarcAuditItem containing the mapped values
   */
  private static MarcAuditItem mapEntityToItem(MarcAuditEntity entity) {
    var item = new MarcAuditItem();
    item.setEventId(entity.eventId());
    item.setEventDate(Date.from(entity.eventDate().atZone(ZoneId.systemDefault()).toInstant()));
    item.setEntityId(entity.entityId());
    item.setAction(entity.action());
    item.setUserId(entity.userId());
    item.setOrigin(entity.origin());
    item.setDiff(entity.diff());
    return item;
  }

  private static RecordData extractRecordData(Record value, String userKey, String action) {
    return new RecordData(
      value.getMatchedId(),
      getUserIdFromMetadata(value.getMetadata(), userKey),
      action
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
    var condition = type.equals(SourceRecordDomainEventType.SOURCE_RECORD_CREATED);
    flattenFields(parsedRecord).forEach((key, value) ->
      fields.add(Map.of(FIELD_KEY, key, condition ? NEW_VALUE_KEY : OLD_VALUE_KEY, value))
    );
    return condition
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
   * Compares two maps of parsed records and identifies the differences between them.
   * The method determines the added, modified, and removed entries by analyzing
   * the keys and values in the input maps. The results are aggregated into categorized lists
   * and returned as a single map.
   *
   * @param oldMap a map representing the original state of parsed records
   * @param newMap a map representing the updated state of parsed records
   * @return a map containing three lists:
   * - "added" for entries present in the new map but not in the old map
   * - "removed" for entries present in the old map but not in the new map
   * - "modified" for
   */
  private static Map<String, Object> compareParsedRecords(Map<String, Object> oldMap, Map<String, Object> newMap) {
    List<Map<String, Object>> added = new ArrayList<>();
    List<Map<String, Object>> modified = new ArrayList<>();
    List<Map<String, Object>> removed = new ArrayList<>();

    populateEntries(newMap, oldMap, added, NEW_VALUE_KEY);
    populateEntries(oldMap, newMap, removed, OLD_VALUE_KEY);
    processModifiedEntries(newMap, oldMap, added, removed, modified);
    return toMap(added, removed, modified);
  }

  /**
   * Populates the provided list with entries that are present in the source map but not in the target map.
   * The method iterates over the key-value pairs in the source map, comparing them to the target map.
   * If a key is present in the source map but not in the target map, the method adds the key-value pair
   * to the provided result list, associating the value with the specified key.
   *
   * @param sourceMap  the map containing the source key-value pairs
   * @param targetMap  the map containing the target key-value pairs
   * @param resultList the list to store entries that are present in the source map but not in the target map
   * @param valueKey   the key to use for associating the value in the result list
   */
  private static void populateEntries(Map<String, Object> sourceMap, Map<String, Object> targetMap,
                                      List<Map<String, Object>> resultList, String valueKey) {
    sourceMap.forEach((key, value) -> {
      if (!targetMap.containsKey(key)) {
        resultList.add(Map.of(FIELD_KEY, key, valueKey, value));
      }
    });
  }

  /**
   * Processes the differences between two maps representing the new and old states of a dataset.
   * The method identifies added, removed, and modified entries by comparing the key-value pairs
   * in the input maps and categorizes them into the respective provided lists.
   *
   * @param newMap   the map containing the new state of the dataset
   * @param oldMap   the map containing the old state of the dataset
   * @param added    a list to store entries that are present in the new map but not in the old map
   * @param removed  a list to store entries that are present in the old map but not in the new map
   * @param modified a list to store entries that exist in both maps but have different values
   */
  private static void processModifiedEntries(Map<String, Object> newMap, Map<String, Object> oldMap,
                                             List<Map<String, Object>> added, List<Map<String, Object>> removed,
                                             List<Map<String, Object>> modified) {
    newMap.forEach((key, newValue) -> {
      if (!FIELD_005.equals(key) && oldMap.containsKey(key)) {
        var oldValue = oldMap.get(key);
        if (!Objects.equals(oldValue, newValue)) {
          var modifiedField = getModifiedFieldMap(key, oldValue, newValue);
          if (!modifiedField.containsKey(OLD_VALUE_KEY)) {
            added.add(modifiedField);
          } else if (!modifiedField.containsKey(NEW_VALUE_KEY)) {
            removed.add(modifiedField);
          } else {
            modified.add(modifiedField);
          }
        }
      }
    });
  }

  /**
   * Generates a map representing the differences between an old and a new value for a specified field key.
   * The method computes the added, removed, and modified elements between the old and new values,
   * returning a map containing the field key and any relevant changes.
   *
   * @param key      the name of the field being evaluated for changes
   * @param oldValue the previous value of the field, which can be a single object or a list of objects
   * @param newValue the updated value of the field, which can be a single object or a list of objects
   * @return a map containing the field key and detected differences between the old and new values.
   *         The map may include:
   *         - "fieldKey" with the provided key
   *         - "oldValue" with the previous data if applicable
   *         - "newValue" with the updated data if applicable
   */
  private static Map<String, Object> getModifiedFieldMap(String key, Object oldValue, Object newValue) {
    var oldList = convertToList(oldValue);
    var newList = convertToList(newValue);

    var oldSet = new HashSet<>(oldList);
    var newSet = new HashSet<>(newList);

    var removedSet = new HashSet<>(oldSet);
    var addedSet = new HashSet<>(newSet);

    removedSet.removeAll(newSet);
    addedSet.removeAll(oldSet);

    var oldResult = formatSingleOrList(removedSet);
    var newResult = formatSingleOrList(addedSet);

    var map = new HashMap<String, Object>();
    map.put(FIELD_KEY, key);

    if (oldList.size() == newList.size()) {
      if (!Objects.equals(oldValue, newValue)) {
        map.put(OLD_VALUE_KEY, oldValue);
        map.put(NEW_VALUE_KEY, newValue);
      }
    } else {
      if (removedSet.isEmpty()) {
        map.put(NEW_VALUE_KEY, newResult);
      } else if (addedSet.isEmpty()) {
        map.put(OLD_VALUE_KEY, oldResult);
      } else {
        map.put(OLD_VALUE_KEY, oldResult);
        map.put(NEW_VALUE_KEY, newResult);
      }
    }
    return map;
  }

  private static Object formatSingleOrList(Set<Object> set) {
    return set.size() == 1 ? set.iterator().next() : new ArrayList<>(set);
  }

  private static List<Object> convertToList(Object value) {
    return value instanceof List<?> ? new ArrayList<>((List<?>) value) : List.of(value);
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

  private record RecordData(String recordId, String userId, String action) {
  }

}
