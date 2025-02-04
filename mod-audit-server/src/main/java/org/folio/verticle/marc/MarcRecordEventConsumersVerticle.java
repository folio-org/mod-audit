package org.folio.verticle.marc;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarcRecordEventConsumersVerticle extends AbstractConsumersVerticle {

  private static final String SOURCE_RECORD_TOPIC = "srs.source_records";

  @Autowired
  private AsyncRecordHandler<String, String> marcRecordEventsHandler;

  @Override
  public List<String> getEvents() {
    return List.of(SOURCE_RECORD_TOPIC);
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return marcRecordEventsHandler;
  }

  @Override
  protected SubscriptionDefinition subscriptionDefinition(String event, KafkaConfig kafkaConfig) {
    return SubscriptionDefinition.builder()
      .eventType(event)
      .subscriptionPattern(String.join("\\.", kafkaConfig.getEnvId(), "\\w{1,}", event))
      .build();
  }
}
