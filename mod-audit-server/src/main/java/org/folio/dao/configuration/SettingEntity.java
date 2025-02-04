package org.folio.dao.configuration;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettingEntity {

  public static final String ID_COLUMN = "id";
  public static final String KEY_COLUMN = "key";
  public static final String VALUE_COLUMN = "value";
  public static final String TYPE_COLUMN = "type";
  public static final String DESCRIPTION_COLUMN = "description";
  public static final String GROUP_ID_COLUMN = "group_id";
  public static final String CREATED_DATE_COLUMN = "created_date";
  public static final String CREATED_BY_COLUMN = "created_by";
  public static final String UPDATED_DATE_COLUMN = "updated_date";
  public static final String UPDATED_BY_COLUMN = "updated_by";

  private String id;
  private String key;
  private Object value;
  private SettingValueType type;
  private String description;
  private String groupId;
  private LocalDateTime createdDate;
  private UUID createdByUserId;
  private LocalDateTime updatedDate;
  private UUID updatedByUserId;
}
