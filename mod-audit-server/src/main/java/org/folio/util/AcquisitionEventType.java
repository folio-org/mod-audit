package org.folio.util;

public enum AcquisitionEventType {
  ACQ_ORDER_CHANGED("ACQ_ORDER_CHANGED"),
  ACQ_ORDER_LINE_CHANGED("ACQ_ORDER_LINE_CHANGED"),
  ACQ_PIECE_CHANGED("ACQ_PIECE_CHANGED");

  private final String topicName;

  AcquisitionEventType(String value) {
    this.topicName = value;
  }

  public String getTopicName() {
    return topicName;
  }
}
