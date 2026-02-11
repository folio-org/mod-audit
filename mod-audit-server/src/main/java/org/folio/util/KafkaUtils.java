package org.folio.util;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaUtils {

  private static final String TOPIC_PATTERN_FORMAT = "(%s\\.)(.*\\.)%s";

  public static String getTopicName(KafkaConsumerRecord<?, ?> consumerRecord) {
    var topic = consumerRecord.topic();
    return topic.substring(topic.indexOf('.', topic.indexOf('.') + 1) + 1);
  }

  public static String formatTopicPattern(String env, String topicPattern) {
    return TOPIC_PATTERN_FORMAT.formatted(env, topicPattern);
  }
}
