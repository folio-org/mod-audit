package org.folio.services.configuration;

import lombok.Getter;

@Getter
public enum SettingGroup {

  AUTHORITY("audit.authority"),
  INVENTORY("audit.inventory");

  private final String id;

  SettingGroup(String id) {
    this.id = id;
  }
}
