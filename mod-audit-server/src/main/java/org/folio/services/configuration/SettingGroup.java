package org.folio.services.configuration;

import lombok.Getter;

@Getter
public enum SettingGroup {

  AUTHORITY("audit.authority"),
  INVENTORY("audit.inventory"),
  USER("audit.users");

  private final String id;

  SettingGroup(String id) {
    this.id = id;
  }
}
