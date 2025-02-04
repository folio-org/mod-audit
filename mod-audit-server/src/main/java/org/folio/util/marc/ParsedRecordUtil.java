package org.folio.util.marc;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import org.folio.dao.marc.MarcAuditEntity;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
@SuppressWarnings("unchecked")
public class ParsedRecordUtil {
  private static final String PARSED_RECORD_KEY = "parsedRecord";
  private static final String SUBFIELDS_KEY = "subfields";
  private static final String FIELD_KEY = "field";
  private static final String FIELDS_KEY = "fields";
  private static final String LEADER_KEY = "leader";
  private static final String VALUE_KEY = "value";

  public static MarcAuditEntity mapToEntity(SourceRecordDomainEvent event, String userId) {
    Map<String, Object> difference = null;
    var eventPayload = event.getEventPayload();
    if (event.getEventType().equals(SourceRecordDomainEventType.SOURCE_RECORD_CREATED)) {
      var newRecord = (Map<String, Object>) eventPayload.getNewRecord().get(PARSED_RECORD_KEY);
      difference = sourceCreatedDifference(newRecord);
    } else if (event.getEventType().equals(SourceRecordDomainEventType.SOURCE_RECORD_UPDATED)) {
      var newRecord = (Map<String, Object>) eventPayload.getNewRecord().get(PARSED_RECORD_KEY);
      var oldRecord = (Map<String, Object>) eventPayload.getOld().get(PARSED_RECORD_KEY);
      difference = calculateDifferences(oldRecord, newRecord);
    }
    return new MarcAuditEntity(
      event.getEventId(),
      event.getEventMetadata().getEventDate(),
      event.getRecordId(),
      event.getEventMetadata().getPublishedBy(),
      event.getEventType().getValue(),
      userId,
      difference
    );
  }

  private static Map<String, Object> calculateDifferences(Map<String, Object> oldParsedRecord, Map<String, Object> newParsedRecord) {
    var oldFields = convert(oldParsedRecord);
    var newFields = convert(newParsedRecord);
    return compareParsedRecords(oldFields, newFields);
  }

  private static Map<String, Object> sourceCreatedDifference(Map<String, Object> parsedRecord) {
    List<Map<String, Object>> added = new ArrayList<>();
    convert(parsedRecord).forEach((key, value) ->
      added.add(Map.of(FIELD_KEY, key, VALUE_KEY, value))
    );
    return toMap(added, Collections.emptyList(), Collections.emptyList());
  }

  private static Map<String, Object> convert(Map<String, Object> input) {
    var content = getContent(input);
    var fields = (List<Map<String, Object>>) content.get(FIELDS_KEY);
    var result = new HashMap<String, Object>();
    if (content.containsKey(LEADER_KEY)) {
      result.put("LDR", content.get(LEADER_KEY));
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
      if (!"005".equals(key) && oldMap.containsKey(key)) {
        var oldValue = oldMap.get(key);
        if (!Objects.equals(oldValue, newValue)) {
          modified.add(Map.of(
            FIELD_KEY, key,
            "oldValue", oldValue,
            "newValue", newValue
          ));
        }
      }
    });

    return toMap(added, removed, modified);
  }

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
    return Map.of(
      "added", added,
      "modified", modified,
      "removed", removed
    );
  }
}
