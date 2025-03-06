package org.folio.util.marc;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.domain.diff.ChangeType;
import org.folio.domain.diff.CollectionChangeDto;
import org.folio.domain.diff.CollectionItemChangeDto;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.MarcAuditCollection;
import org.folio.rest.jaxrs.model.MarcAuditItem;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for mapping and processing MARC records and audit-related entities.
 * The MarcUtil class provides static methods for mapping domain events to entities,
 * comparing old and new states of records, identifying differences, and formatting field data.
 */
@UtilityClass
public class MarcUtil {
  private static final String LDR = "LDR";
  private static final String FIELD_005 = "005";
  private static final String FIELD_999 = "999";
  private static final String FF_IND = "ff";
  private static final String SUBFIELDS_KEY = "subfields";
  private static final String FIELDS_KEY = "fields";
  private static final String LEADER_KEY = "leader";
  private static final String CREATED_BY = "createdByUserId";
  private static final String UPDATED_BY = "updatedByUserId";
  private static final String CREATED = "CREATED";
  private static final String UPDATED = "UPDATED";
  private static final String DELETED = "DELETED";
  private static final String SPACE_DELIMITER = " ";
  private static final String SUBFIELD_DELIMITER = " $";

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
    ChangeRecordDto difference;
    var oldRecord = event.getEventPayload().getOld();
    var newRecord = event.getEventPayload().getNewRecord();

    switch (event.getEventType()) {
      case SOURCE_RECORD_CREATED -> {
        data = extractRecordData(newRecord, CREATED_BY, CREATED);
        difference = getDifference(newRecord.getParsedRecord(), ChangeType.ADDED);
      }
      case SOURCE_RECORD_UPDATED -> {
        data = extractRecordData(newRecord, UPDATED_BY, UPDATED);
        difference = calculateDifferences(oldRecord.getParsedRecord(), newRecord.getParsedRecord());
      }
      case SOURCE_RECORD_DELETED -> {
        data = extractRecordData(oldRecord, UPDATED_BY, DELETED);
        difference = getDifference(oldRecord.getParsedRecord(), ChangeType.REMOVED);
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
    item.setEventTs(entity.eventDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    return item;
  }

  private static ChangeRecordDto calculateDifferences(Map<String, Object> oldParsedRecord, Map<String, Object> newParsedRecord) {
    var oldFields = flattenFields(oldParsedRecord);
    var newFields = flattenFields(newParsedRecord);
    return compareParsedRecords(oldFields, newFields);
  }

  /**
   * Generates a {@link ChangeRecordDto} representing the differences in a given parsed record
   * based on the provided change type.
   *
   * <p>This method processes the parsed record by flattening its structure and creating a list of
   * {@link FieldChangeDto} objects for each field. If a field's value is a list, it iterates through
   * the list and creates individual changes for each element. Otherwise, it directly creates a
   * change for the field value. The resulting changes are of the specified {@link ChangeType}.
   *
   * <p>The method uses the flattened structure of the record to create changes for both individual
   * values and collections, depending on the structure of the input map.
   *
   * @param parsedRecord the map representing the parsed record to process
   * @param type         the {@link ChangeType} indicating the type of change (e.g., ADDED, REMOVED)
   * @return a {@link ChangeRecordDto} containing the list of field changes, with no repeatable changes
   */
  private static ChangeRecordDto getDifference(Map<String, Object> parsedRecord, ChangeType type) {
    var changes = new ArrayList<FieldChangeDto>();
    var content = flattenFields(parsedRecord);
    content.forEach((key, value) -> addChangesFromValue(value, key, type, changes));
    return new ChangeRecordDto(changes, Collections.emptyList());
  }

  /**
   * Compares two given maps representing parsed records and identifies the changes between them.
   *
   * <p>This method computes the differences between the two maps (`oldMap` and `newMap`)
   * and categorizes the changes into four categories:
   * <ul>
   *   <li>Added fields: Fields that exist in the `newMap` but not in the `oldMap`.</li>
   *   <li>Removed fields: Fields that exist in the `oldMap` but not in the `newMap`.</li>
   *   <li>Modified fields: Fields that exist in both maps but have different values.</li>
   *   <li>Repeatable fields: Collections (e.g., lists) that were modified, tracked as repeatable changes.</li>
   * </ul>
   *
   * <p>The method returns a {@link ChangeRecordDto} containing the categorized changes, including both
   * individual field changes and repeatable field changes.
   *
   * @param oldMap the map representing the original parsed record (state before changes)
   * @param newMap the map representing the updated parsed record (state after changes)
   * @return a {@link ChangeRecordDto} consisting of field changes (added, removed, modified) and
   * repeatable collection changes
   */
  private static ChangeRecordDto compareParsedRecords(Map<String, Object> oldMap, Map<String, Object> newMap) {
    List<FieldChangeDto> added = new ArrayList<>();
    List<FieldChangeDto> removed = new ArrayList<>();
    List<FieldChangeDto> modified = new ArrayList<>();
    List<CollectionChangeDto> repeatable = new ArrayList<>();

    populateEntries(newMap, oldMap, added, ChangeType.ADDED);
    populateEntries(newMap, oldMap, removed, ChangeType.REMOVED);
    processModifiedEntries(newMap, oldMap, repeatable, modified);

    List<FieldChangeDto> fieldChanges = new ArrayList<>();
    fieldChanges.addAll(added);
    fieldChanges.addAll(removed);
    fieldChanges.addAll(modified);

    return new ChangeRecordDto(fieldChanges, repeatable);
  }

  /**
   * Populates the given list of {@link FieldChangeDto} with changes detected between the source and target maps.
   * This method identifies entries in the source map that do not exist in the target map, and based on their values,
   * adds corresponding change records to the provided list.
   *
   * @param firstMap  the first map, potentially representing either the source or target, depending on the change type.
   * @param secondMap the second map, potentially representing either the source or target, depending on the change type.
   * @param changes   the list of {@link FieldChangeDto} objects to which detected changes will be added.
   * @param type      the {@link ChangeType} indicating the type of change (e.g., ADDED, REMOVED, etc.).
   *                  Determines which map acts as the source and which as the target.
   *
   *                  <p>The method determines the source and target maps based on the change type:
   *                  <ul>
   *                    <li>If type is {@link ChangeType#ADDED}, {@code firstMap} is treated as the source
   *                        and {@code secondMap} as the target.</li>
   *                    <li>Otherwise, {@code secondMap} is treated as the source and {@code firstMap} as the target.</li>
   *                  </ul>
   *
   *                  <p>For every key in the source map that does not exist in the target map:
   *                  <ul>
   *                    <li>If the value associated with the key is a {@link List}, each element of the list
   *                        is treated as a separate change and recorded in the list of {@link FieldChangeDto}.</li>
   *                    <li>For non-list values, the value itself is recorded as a change.</li>
   *                  </ul>
   *
   *                  <p>{@link FieldChangeDto} objects are created using the {@code addChange} helper method,
   *                  which encapsulates the creation and storage of change records.
   */
  private static void populateEntries(Map<String, Object> firstMap, Map<String, Object> secondMap,
                                      List<FieldChangeDto> changes, ChangeType type) {
    var source = ChangeType.ADDED.equals(type) ? firstMap : secondMap;
    var target = ChangeType.ADDED.equals(type) ? secondMap : firstMap;
    source.forEach((key, value) -> {
      if (!target.containsKey(key)) {
        addChangesFromValue(value, key, type, changes);
      }
    });
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
      var ind1 = (String) fieldData.getOrDefault("ind1", SPACE_DELIMITER);
      var ind2 = (String) fieldData.getOrDefault("ind2", SPACE_DELIMITER);
      var subfields = (List<Map<String, Object>>) fieldData.get(SUBFIELDS_KEY);
      var subfieldsString = subfields.stream()
        .flatMap(subObj -> subObj.entrySet().stream()
          .map(e -> SUBFIELD_DELIMITER + e.getKey() + SPACE_DELIMITER + e.getValue()))
        .collect(Collectors.joining());
      return ind1 + ind2 + subfieldsString;
    }
    return value.toString();
  }

  /**
   * Compares old and new values of a field to track changes, including modifications, additions, and removals.
   * The method categorizes changes into field-level and collection-level changes.
   *
   * @param key        The identifier or key of the field being compared.
   * @param oldValue   The old value of the field before the change. Can be a single value or a collection.
   * @param newValue   The new value of the field after the change. Can be a single value or a collection.
   * @param repeatable A list of {@link CollectionChangeDto} to track changes for fields that are collections.
   *                   This includes added and/or removed items.
   * @param changes    A list of {@link FieldChangeDto} to track modifications to single field values
   *                   (e.g., direct changes in non-collection fields).
   */
  private static void populateChanges(
    String key,
    Object oldValue,
    Object newValue,
    List<CollectionChangeDto> repeatable,
    List<FieldChangeDto> changes
  ) {
    var oldList = convertToList(oldValue);
    var newList = convertToList(newValue);

    var removedSet = new HashSet<>(oldList);
    var addedSet = new HashSet<>(newList);

    newList.forEach(removedSet::remove);
    oldList.forEach(addedSet::remove);

    //Case 1: No changes detected
    if (removedSet.isEmpty() && addedSet.isEmpty()) return;

    if (isSingleValueChange(oldList, newList) &&
      !Objects.equals(oldList.get(0), newList.get(0))) {
      changes.add(FieldChangeDto.modified(key, key, oldList.get(0), newList.get(0)));
      return;
    }

    //Case 2: Only addition or removal changes detected
    if (removedSet.isEmpty() || addedSet.isEmpty()) {
      var targetSet = removedSet.isEmpty() ? addedSet : removedSet;
      Function<Object, CollectionItemChangeDto> mapper = removedSet.isEmpty()
        ? CollectionItemChangeDto::added
        : CollectionItemChangeDto::removed;
      addRepeatableChange(key, targetSet, mapper, repeatable);
      return;
    }

    var itemChanges = new ArrayList<CollectionItemChangeDto>();
    removedSet.forEach(item -> itemChanges.add(CollectionItemChangeDto.removed(item)));
    addedSet.forEach(item -> itemChanges.add(CollectionItemChangeDto.added(item)));
    repeatable.add(new CollectionChangeDto(key, itemChanges));
  }

  /**
   * Processes entries in the new map to identify fields that have been modified compared to the old map.
   * For each key in the new map that also exists in the old map, the method checks if the corresponding values
   * are different. If a difference is detected, the method delegates handling of the modification to
   * {@code getModifiedFieldMap}.
   *
   * @param newMap     the map representing the updated state of fields.
   * @param oldMap     the map representing the original state of fields.
   * @param repeatable the list of {@link CollectionChangeDto} objects to store information about repeatable
   *                   field changes, if applicable.
   * @param modified   the list of {@link FieldChangeDto} objects to store information about detected field modifications.
   *
   *                   <p>For each key-value pair in {@code newMap}:
   *                   <ul>
   *                     <li>The method skips processing if the key is equal to {@code FIELD_005} or {@code FIELD_999} with indication {@code FF_IND}, as this field is excluded from modification checks.</li>
   *                     <li>If the key also exists in {@code oldMap}, the method further checks if the values in both maps are different using {@link Objects#equals}.</li>
   *                     <li>If a difference is detected, the method calls {@code getModifiedFieldMap} to handle the detected change.</li>
   *                   </ul>
   *
   *                   <p>Detected field modifications are typically categorized into repeatable and non-repeatable changes:
   *                   <ul>
   *                     <li>Repeatable field changes are added to the {@code repeatable} list as {@link CollectionChangeDto} objects.</li>
   *                     <li>Non-repeatable field changes are added to the {@code modified} list as {@link FieldChangeDto} objects.</li>
   *                   </ul>
   */
  private static void processModifiedEntries(
    Map<String, Object> newMap,
    Map<String, Object> oldMap,
    List<CollectionChangeDto> repeatable,
    List<FieldChangeDto> modified
  ) {
    newMap.forEach((key, newValue) -> {
      if (!FIELD_005.equals(key) && oldMap.containsKey(key)) {
        var oldValue = oldMap.get(key);
        if (FIELD_999.equals(key)) {
          var filteredOld = filterNonFFValues(oldValue);
          var filteredNew = filterNonFFValues(newValue);
          if (filteredOld.isEmpty() && filteredNew.isEmpty()) return;
          if (!Objects.equals(filteredOld, filteredNew)) {
            populateChanges(key, filteredOld, filteredNew, repeatable, modified);
          }
        } else if (!Objects.equals(oldValue, newValue)) {
          populateChanges(key, oldValue, newValue, repeatable, modified);
        }
      }
    });
  }

  /**
   * Adds a repeatable field change to the list of repeatable changes.
   *
   * <p>This method is used when either additions or removals are detected in repeatable MARC fields.
   * It applies the provided mapper function (either {@link CollectionItemChangeDto#added} or {@link CollectionItemChangeDto#removed})
   * to each element in the given set, creating a list of {@link CollectionItemChangeDto}.
   *
   * @param key          the MARC field tag (e.g., "020") that the changes apply to.
   * @param targetSet    the set of values that were either added or removed.
   * @param changeMapper a function defining how to map the items (either as additions or removals).
   * @param repeatable   the list to which the created {@link CollectionChangeDto} is added.
   */
  private static void addRepeatableChange(
    String key,
    Set<Object> targetSet,
    Function<Object, CollectionItemChangeDto> changeMapper,
    List<CollectionChangeDto> repeatable
  ) {
    List<CollectionItemChangeDto> itemChanges = targetSet.stream()
      .map(changeMapper)
      .toList();
    repeatable.add(new CollectionChangeDto(key, itemChanges));
  }

  /**
   * Adds individual field changes to the provided list of {@link FieldChangeDto}.
   *
   * <p>This method checks if the provided value is a list (repeatable field) or a single value.
   * For lists, it iterates over each element and creates separate change entries.
   * For single values, it adds the change directly.
   *
   * <p>The change type (added or removed) determines whether the value is treated as an addition or removal.
   *
   * @param changes the list to which the created {@link FieldChangeDto} entries are added.
   * @param type    the type of change ({@link ChangeType#ADDED} or {@link ChangeType#REMOVED}).
   * @param key     the MARC field tag (e.g., "020") that the change applies to.
   * @param value   the value or list of values representing the field content.
   */
  private static void addChangesFromValue(Object value, String key, ChangeType type, List<FieldChangeDto> changes) {
    if (value instanceof List<?>) {
      ((List<?>) value).forEach(v -> addChange(changes, type, key, v));
    } else {
      addChange(changes, type, key, value);
    }
  }

  private static List<Object> filterNonFFValues(Object value) {
    return convertToList(value).stream()
      .filter(v -> !(v instanceof String str && str.startsWith(FF_IND)))
      .toList();
  }

  private static boolean isSingleValueChange(List<Object> oldList, List<Object> newList) {
    return oldList.size() == 1 && newList.size() == 1;
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

  private static void addChange(List<FieldChangeDto> changes, ChangeType type, String key, Object value) {
    var fieldChange = ChangeType.ADDED.equals(type) ? FieldChangeDto.added(key, key, value) : FieldChangeDto.removed(key, key, value);
    changes.add(fieldChange);
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

  private record RecordData(String recordId, String userId, String action) {
  }
}
