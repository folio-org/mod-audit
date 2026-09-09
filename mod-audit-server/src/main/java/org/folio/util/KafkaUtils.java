package org.folio.util;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.rest.tools.utils.TenantTool;

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

  /**
   * Extracts the tenant id from a Kafka consumer record's headers.
   * <p>
   * Kafka message headers are propagated by producers using the lowercase
   * {@code x-okapi-*} header keys assigned by RMB at HTTP ingress, whereas
   * {@link KafkaHeaderUtils#kafkaHeadersToMap} returns a plain case-sensitive
   * map. Wrapping the result in a {@link CaseInsensitiveMap} keeps tenant
   * resolution consistent with the REST path, where RMB already hands
   * business logic a case-insensitive headers map.
   *
   * @param consumerRecord the Kafka consumer record to extract headers from
   * @return resolved tenant id, or {@code TenantTool}'s default if not present
   */
  public static String getTenantId(KafkaConsumerRecord<?, ?> consumerRecord) {
    var headers = new CaseInsensitiveMap<>(KafkaHeaderUtils.kafkaHeadersToMap(consumerRecord.headers()));
    return TenantTool.tenantId(headers);
  }

  /**
   * Converts a Kafka consumer record's headers into a case-insensitive
   * {@code Map<String, String>}, suitable for passing to code (such as
   * {@code LogRecordBuilder}) that expects an RMB-style okapiHeaders map.
   *
   * @param consumerRecord the Kafka consumer record to extract headers from
   * @return case-insensitive map of okapi headers
   */
  public static Map<String, String> getOkapiHeaders(KafkaConsumerRecord<?, ?> consumerRecord) {
    return new CaseInsensitiveMap<>(KafkaHeaderUtils.kafkaHeadersToMap(consumerRecord.headers()));
  }
}
