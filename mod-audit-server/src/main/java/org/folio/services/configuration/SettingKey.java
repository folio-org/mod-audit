package org.folio.services.configuration;

import lombok.Getter;


@Getter
public enum SettingKey {

  ENABLED("enabled"),
  RECORDS_PAGE_SIZE("records.page.size"),
  RETENTION_PERIOD("records.retention.period");

  private final String value;

  SettingKey(String value) {
    this.value = value;
  }
}
