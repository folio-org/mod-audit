package org.folio.verticle.marc;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.verticle.AbstractConsumersVerticle;
import org.folio.verticle.marc.consumers.MarcRecordEventsHandler;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarcRecordEventConsumersVerticle extends AbstractConsumersVerticle {

  private final ObjectFactory<MarcRecordEventsHandler> recordHandlerProvider;
  private static final String SOURCE_RECORD_TOPIC = "srs.source_records";

  public MarcRecordEventConsumersVerticle(ObjectFactory<MarcRecordEventsHandler> recordHandlerProvider) {
    this.recordHandlerProvider = recordHandlerProvider;
  }

  @Override
  public List<String> getEvents() {
    return List.of(SOURCE_RECORD_TOPIC);
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return recordHandlerProvider.getObject();
  }

  @Override
  protected SubscriptionDefinition subscriptionDefinition(String event, KafkaConfig kafkaConfig) {
    return SubscriptionDefinition.builder()
      .eventType(event)
      .subscriptionPattern(String.join("\\.", kafkaConfig.getEnvId(), "\\w{1,}", event))
      .build();
  }
}
