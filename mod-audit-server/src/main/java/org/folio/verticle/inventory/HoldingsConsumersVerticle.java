package org.folio.verticle.inventory;

import static org.folio.util.inventory.InventoryUtils.formatInventoryTopicPattern;

import java.util.List;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.util.inventory.InventoryKafkaEvent;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HoldingsConsumersVerticle extends AbstractConsumersVerticle {

  @Autowired
  private AsyncRecordHandler<String, String> inventoryEventHandler;

  @Override
  public List<String> getEvents() {
    return List.of(InventoryKafkaEvent.HOLDINGS.getTopicName());
  }

  @Override
  protected SubscriptionDefinition subscriptionDefinition(String event, KafkaConfig kafkaConfig) {
    return SubscriptionDefinition.builder()
      .eventType(event)
      .subscriptionPattern(formatInventoryTopicPattern(kafkaConfig.getEnvId(), InventoryKafkaEvent.HOLDINGS))
      .build();
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return inventoryEventHandler;
  }
}
