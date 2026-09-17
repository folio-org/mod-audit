package org.folio.util;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.tools.utils.TenantTool;

@UtilityClass
public class KafkaUtils {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String SYSTEM_USER_ENV_VAR = "SYSTEM_USER_ENABLED";
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
    var headers = getConsumerRecordHeaders(consumerRecord);
    return TenantTool.tenantId(headers);
  }

  /**
   * Converts a Kafka consumer record's headers into a case-insensitive
   * {@code Map<String, String>}, suitable for passing to code that expects an RMB-style headers map.
   *
   * @param consumerRecord the Kafka consumer record to extract headers from
   * @return case-insensitive map of consumer record headers
   */
  public static Map<String, String> getConsumerRecordHeaders(KafkaConsumerRecord<?, ?> consumerRecord) {
    var headers = KafkaHeaderUtils.kafkaHeadersToMap(consumerRecord.headers());
    if (isSystemUserEnabled()) {
      var tenant = headers.get(XOkapiHeaders.TENANT);
      LOGGER.debug("getConsumerRecordHeaders:: Creating headers map without token for system user, tenant: {}", tenant);
      headers.remove(XOkapiHeaders.TOKEN);
    }
    return new CaseInsensitiveMap<>(headers);
  }

  /**
   * Checks if the system user is enabled based on a system property.
   * This method reads the `SYSTEM_USER_ENABLED` system property and parses
   * its value as a boolean. If the property is not found or cannot be parsed,
   * it defaults to `true`. The method then negates the parsed value and returns it.
   * Note: SYSTEM_USER_ENABLED defines if system user must be created at service tenant initialization and
   * this functionality is specific to the Eureka environment.
   *
   * @return {@code true} if the system user is set for Eureka env; otherwise {@code false}.
   */
  private static boolean isSystemUserEnabled() {
    return !Boolean.parseBoolean(System.getenv().getOrDefault(SYSTEM_USER_ENV_VAR,
      System.getProperty(SYSTEM_USER_ENV_VAR, "true")));
  }
}
