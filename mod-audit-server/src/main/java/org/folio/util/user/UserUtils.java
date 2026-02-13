package org.folio.util.user;

import lombok.experimental.UtilityClass;
import org.folio.util.KafkaUtils;
import org.folio.util.PayloadUtils;

@UtilityClass
public class UserUtils {

  public static String extractPerformedBy(UserEvent event) {
    var payload = event.getNewValue() != null ? event.getNewValue() : event.getOldValue();
    return PayloadUtils.extractPerformedByUserId(payload);
  }

  public static String formatUserTopicPattern(String env, UserKafkaEvent eventType) {
    return KafkaUtils.formatTopicPattern(env, eventType.getTopicPattern());
  }
}
