package org.folio.verticle.inventory;

import java.util.List;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.util.inventory.InventoryKafkaEvent;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InstanceConsumersVerticle extends AbstractConsumersVerticle {

  @Autowired
  private AsyncRecordHandler<String, String> inventoryEventHandler;

  @Override
  public List<String> getEvents() {
    return List.of(InventoryKafkaEvent.INSTANCE.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return inventoryEventHandler;
  }
}
