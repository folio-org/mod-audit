package org.folio.util.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryEvent {

  private String entityId;
  private String eventId;
  private String tenant;
  private InventoryEventType type;
  private InventoryResourceType resourceType;
  private InventoryDeleteEventSubType deleteEventSubType;
  private Long eventTs;
  private Boolean isConsortiumShadowCopy;

  @JsonProperty("new")
  private Map<String, Object> newValue;
  @JsonProperty("old")
  private Map<String, Object> oldValue;
}
