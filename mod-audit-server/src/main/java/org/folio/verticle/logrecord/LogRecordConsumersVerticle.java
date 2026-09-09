package org.folio.verticle.logrecord;

import java.util.List;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.util.AuditKafkaTopic;
import org.folio.util.KafkaUtils;
import org.folio.verticle.AbstractConsumersVerticle;
import org.folio.verticle.logrecord.consumers.LogRecordEventHandler;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogRecordConsumersVerticle extends AbstractConsumersVerticle {

  private final ObjectFactory<LogRecordEventHandler> recordHandlerProvider;

  @Value("${audit.log-record.kafka.consumer.loadLimit:10}")
  private int loadLimit;

  public LogRecordConsumersVerticle(ObjectFactory<LogRecordEventHandler> recordHandlerProvider) {
    this.recordHandlerProvider = recordHandlerProvider;
  }

  @Override
  protected SubscriptionDefinition subscriptionDefinition(String event, KafkaConfig kafkaConfig) {
    var topicPattern = AuditKafkaTopic.LOG_RECORD.moduleTopicName().replace(".", "\\.");
    return SubscriptionDefinition.builder()
      .eventType(event)
      .subscriptionPattern(KafkaUtils.formatTopicPattern(kafkaConfig.getEnvId(), topicPattern))
      .build();
  }

  @Override
  protected int getLoadLimit() {
    return loadLimit;
  }

  @Override
  public List<String> getEvents() {
    return List.of(AuditKafkaTopic.LOG_RECORD.moduleTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return recordHandlerProvider.getObject();
  }
}
