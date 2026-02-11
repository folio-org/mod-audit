package org.folio.util.inventory;

import static org.apache.commons.collections4.MapUtils.getString;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.folio.util.KafkaUtils;
import org.folio.util.PayloadUtils;

@UtilityClass
public class InventoryUtils {

  public static final String DEFAULT_ID = "00000000-0000-0000-0000-000000000000";

  private static final String CONSORTIUM_SOURCE = "CONSORTIUM-";

  private static final String SOURCE_FIELD = "source";

  public static String extractUserId(InventoryEvent event) {
    var payload = getEventPayload(event);
    var id = PayloadUtils.extractPerformedByUserId(payload);
    return id != null ? id : DEFAULT_ID;
  }

  public static Map<String, Object> getEventPayload(InventoryEvent event) {
    return event.getNewValue() != null
      ? event.getNewValue()
      : event.getOldValue();
  }

  public static String formatInventoryTopicPattern(String env, InventoryKafkaEvent eventType) {
    return KafkaUtils.formatTopicPattern(env, eventType.getTopicPattern());
  }

  public static boolean isShadowCopyEvent(InventoryEvent event) {
    var payload = getEventPayload(event);
    var source = getString(payload, SOURCE_FIELD);
    return source != null && source.startsWith(CONSORTIUM_SOURCE);
  }
}
