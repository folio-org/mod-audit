package org.folio.util.inventory;

import lombok.Getter;

public enum InventoryResourceType {
  INSTANCE("instance"),
  HOLDINGS("holdings"),
  ITEM("item"),
  UNKNOWN("unknown");

  @Getter
  private final String type;

  InventoryResourceType(String type) {
    this.type = type;
  }
}
