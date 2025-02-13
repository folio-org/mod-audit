package org.folio.services.configuration;

import lombok.Getter;

@Getter
public enum SettingGroup {

  AUTHORITY("audit.authority"),
  INVENTORY("audit.inventory"),
  MARC("audit.marc");

  private final String id;

  SettingGroup(String id) {
    this.id = id;
  }
}
