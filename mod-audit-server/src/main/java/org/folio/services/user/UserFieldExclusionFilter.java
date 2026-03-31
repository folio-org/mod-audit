package org.folio.services.user;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.domain.diff.CollectionChangeDto;
import org.folio.domain.diff.FieldChangeDto;

public final class UserFieldExclusionFilter {

  private static final Logger LOGGER = LogManager.getLogger();

  private UserFieldExclusionFilter() {
  }

  /**
   * Parses the raw setting value into a set of excluded field paths.
   * Accepts any type; returns an empty set for null, non-String, blank, or malformed values.
   */
  public static Set<String> parseExcludedFields(Object settingValue) {
    if (!(settingValue instanceof String value) || value.isBlank()) {
      if (settingValue != null && !(settingValue instanceof String)) {
        LOGGER.warn("parseExcludedFields:: Unexpected value type [type: {}]",
          settingValue.getClass().getSimpleName());
      }
      return Collections.emptySet();
    }
    try {
      return new JsonArray(value).stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .collect(Collectors.toSet());
    } catch (DecodeException e) {
      LOGGER.warn("parseExcludedFields:: Failed to parse excluded fields value, returning empty [value: '{}']",
        value, e);
      return Collections.emptySet();
    }
  }

  /**
   * Filters the given diff by removing entries whose fully-qualified dot-notation fullPath
   * matches an excluded path (exact match or parent prefix).
   *
   * @return the filtered diff, or {@code null} if all changes were excluded or the input was null
   */
  public static ChangeRecordDto applyExclusion(ChangeRecordDto diff, Set<String> excludedPaths) {
    if (diff == null || excludedPaths.isEmpty()) {
      return diff;
    }

    var fieldChanges = filter(diff.getFieldChanges(), FieldChangeDto::getFullPath, excludedPaths);
    var collectionChanges = filter(diff.getCollectionChanges(), CollectionChangeDto::getFullPath, excludedPaths);

    if (fieldChanges.isEmpty() && collectionChanges.isEmpty()) {
      return null;
    }
    return new ChangeRecordDto(fieldChanges, collectionChanges);
  }

  private static boolean isExcluded(String fullPath, Set<String> excludedPaths) {
    return excludedPaths.contains(fullPath)
      || excludedPaths.stream().anyMatch(path -> fullPath.startsWith(path + "."));
  }

  private static <T> List<T> filter(List<T> items, Function<T, String> pathExtractor,
                                     Set<String> excludedPaths) {
    if (items == null) {
      return List.of();
    }
    return items.stream()
      .filter(item -> !isExcluded(pathExtractor.apply(item), excludedPaths))
      .toList();
  }
}
