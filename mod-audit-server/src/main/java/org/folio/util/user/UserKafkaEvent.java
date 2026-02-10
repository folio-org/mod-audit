package org.folio.util.user;

public enum UserKafkaEvent {
  USER("users");

  private static final String TOPIC_GROUP = "users";
  private final String topicName;

  UserKafkaEvent(String value) {
    this.topicName = value;
  }

  public String getTopicName() {
    return TOPIC_GROUP + "." + topicName;
  }

  public String getTopicPattern() {
    return TOPIC_GROUP + "\\." + topicName;
  }
}
