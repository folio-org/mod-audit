package org.folio.services.configuration;

import lombok.Getter;

@Getter
public enum Setting {
  AUTHORITY_RECORDS_PAGE_SIZE(SettingGroup.AUTHORITY, Setting.RECORDS_PAGE_SIZE_KEY),
  INVENTORY_RECORDS_PAGE_SIZE(SettingGroup.INVENTORY, Setting.RECORDS_PAGE_SIZE_KEY);

  public static final String RECORDS_PAGE_SIZE_KEY = "records.page.size";

  private final String key;
  private final SettingGroup group;

  Setting(SettingGroup group, String key) {
    this.group = group;
    this.key = key;
  }

  public String getSettingId() {
    return group.getId() + "." + key;
  }
}
