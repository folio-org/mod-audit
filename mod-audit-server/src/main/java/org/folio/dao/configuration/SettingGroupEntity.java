package org.folio.dao.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingGroupEntity {

  public static final String ID_COLUMN = "id";
  public static final String NAME_COLUMN = "name";
  public static final String DESCRIPTION_COLUMN = "description";
  private String id;
  private String name;
  private String description;
}
