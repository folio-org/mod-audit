package org.folio.verticle.inventory;

import static org.folio.util.inventory.InventoryUtils.formatInventoryTopicPattern;

import java.util.List;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.util.inventory.InventoryKafkaEvent;
import org.folio.verticle.AbstractConsumersVerticle;
import org.folio.verticle.inventory.consumers.InventoryEventHandler;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

@Component
public class ItemConsumersVerticle extends AbstractConsumersVerticle {

  private final ObjectFactory<InventoryEventHandler> recordHandlerProvider;

  public ItemConsumersVerticle(ObjectFactory<InventoryEventHandler> recordHandlerProvider) {
    this.recordHandlerProvider = recordHandlerProvider;
  }

  @Override
  protected SubscriptionDefinition subscriptionDefinition(String event, KafkaConfig kafkaConfig) {
    return SubscriptionDefinition.builder()
      .eventType(event)
      .subscriptionPattern(formatInventoryTopicPattern(kafkaConfig.getEnvId(), InventoryKafkaEvent.ITEM))
      .build();
  }

  @Override
  public List<String> getEvents() {
    return List.of(InventoryKafkaEvent.ITEM.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return recordHandlerProvider.getObject();
  }
}
