package org.folio.utils;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.folio.kafka.KafkaConfig;
import org.folio.rest.RestVerticle;
import org.folio.util.AuditKafkaTopic;

/**
 * Test-only helper for publishing LOG_RECORD Kafka messages to the embedded
 * {@link org.testcontainers.kafka.KafkaContainer} started by {@code TestSuite}, so integration
 * tests can seed/exercise data through the real Kafka consumer pipeline
 * ({@code LogRecordConsumersVerticle} / {@code LogRecordEventHandler})
 */
@UtilityClass
public class KafkaTestProducerUtil {

  private static final int PUBLISH_TIMEOUT_SECONDS = 10;

  /**
   * Publishes a single LOG_RECORD Kafka message and blocks until the broker acknowledges it.
   *
   * @param vertx       the Vertx instance backing the shared producer (typically {@code TestSuite.getVertx()})
   * @param tenantId    tenant id used both to build the topic name and to propagate the
   *                    {@code X-Okapi-Tenant} header
   * @param payloadJson raw JSON string of the message value, e.g. the contents of a sample
   *                    {@code payloads/*.json} resource file (must contain a top-level
   *                    {@code logEventType} field, as expected by {@code LogRecordEventHandler})
   */
  public static void publishLogRecordEvent(Vertx vertx, String tenantId, String payloadJson) {
    var kafkaConfig = buildKafkaConfig();
    var topic = AuditKafkaTopic.LOG_RECORD.fullTopicName(tenantId);
    var producer = KafkaProducer.<String, String>createShared(vertx, "test-log-record-producer", kafkaConfig.getProducerProps());

    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, UUID.randomUUID().toString(), payloadJson);
    record.addHeader(RestVerticle.OKAPI_HEADER_TENANT, tenantId);

    sendAndAwait(producer, record);
  }

  /**
   * Publishes several LOG_RECORD Kafka messages, waiting for each broker acknowledgement in turn.
   *
   * @param vertx        the Vertx instance backing the shared producer
   * @param tenantId     tenant id used both to build the topic name and to propagate the
   *                     {@code X-Okapi-Tenant} header
   * @param payloadJsons raw JSON strings of the message values to publish, in order
   */
  public static void publishLogRecordEvents(Vertx vertx, String tenantId, List<String> payloadJsons) {
    payloadJsons.forEach(payloadJson -> publishLogRecordEvent(vertx, tenantId, payloadJson));
  }

  private static void sendAndAwait(KafkaProducer<String, String> producer, KafkaProducerRecord<String, String> record) {
    CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
    producer.send(record)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          future.complete(ar.result());
        } else {
          future.completeExceptionally(ar.cause());
        }
      });
    try {
      future.get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to publish LOG_RECORD Kafka message", e);
    }
  }

  private static KafkaConfig buildKafkaConfig() {
    return KafkaConfig.builder()
      .envId(System.getProperty("ENV", "folio"))
      .kafkaHost(System.getProperty("KAFKA_HOST", "kafka"))
      .kafkaPort(System.getProperty("KAFKA_PORT", "9092"))
      .build();
  }
}
