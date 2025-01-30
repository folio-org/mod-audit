package org.folio.util;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaUtils {

  public static String getTopicName(KafkaConsumerRecord<?, ?> consumerRecord) {
    var topic = consumerRecord.topic();
    return topic.substring(topic.indexOf('.', topic.indexOf('.') + 1) + 1);
  }
}
