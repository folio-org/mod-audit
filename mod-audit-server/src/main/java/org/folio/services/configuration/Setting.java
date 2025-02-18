package org.folio.services.configuration;

import lombok.Getter;

@Getter
public enum Setting {

  AUTHORITY_RECORDS_PAGE_SIZE(SettingGroup.AUTHORITY, SettingKey.RECORDS_PAGE_SIZE),
  INVENTORY_RECORDS_PAGE_SIZE(SettingGroup.INVENTORY, SettingKey.RECORDS_PAGE_SIZE),
  AUTHORITY_RECORDS_RETENTION_PERIOD(SettingGroup.AUTHORITY, SettingKey.RETENTION_PERIOD),
  INVENTORY_RECORDS_RETENTION_PERIOD(SettingGroup.INVENTORY, SettingKey.RETENTION_PERIOD),
  INVENTORY_RECORDS_ENABLED(SettingGroup.INVENTORY, SettingKey.ENABLED),
  AUTHORITY_RECORDS_ENABLED(SettingGroup.AUTHORITY, SettingKey.ENABLED);

  private final SettingGroup group;
  private final SettingKey key;

  Setting(SettingGroup group, SettingKey key) {
    this.group = group;
    this.key = key;
  }

  public String getSettingId() {
    return group.getId() + "." + key.getValue();
  }
}
