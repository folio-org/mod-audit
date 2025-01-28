package org.folio.util.inventory;

public enum InventoryKafkaEvent {
  INSTANCE("instance"),
  HOLDINGS("holdings-record"),
  ITEM("item");

  private static final String TOPIC_GROUP = "inventory";
  private final String topicName;

  InventoryKafkaEvent(String value) {
    this.topicName = value;
  }

  public String getTopicName() {
    return TOPIC_GROUP + "." + topicName;
  }

  public String getTopicPattern() {
    return TOPIC_GROUP + "\\." + topicName;
  }
}
