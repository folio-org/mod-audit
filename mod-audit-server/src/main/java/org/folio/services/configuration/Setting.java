package org.folio.services.configuration;

import lombok.Getter;

@Getter
public enum Setting {

  AUTHORITY_RECORDS_PAGE_SIZE(SettingGroup.AUTHORITY, "records.page.size"),
  INVENTORY_RECORDS_PAGE_SIZE(SettingGroup.INVENTORY, "records.page.size");

  private final SettingGroup group;
  private final String key;

  Setting(SettingGroup group, String key) {
    this.group = group;
    this.key = key;
  }
}
