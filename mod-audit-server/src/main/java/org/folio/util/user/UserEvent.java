package org.folio.util.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEvent {

  private String id;
  private UserEventType type;
  private String tenant;
  private Long timestamp;
  private String userId;
  private Map<String, Object> oldValue;
  private Map<String, Object> newValue;

  @JsonSetter("data")
  @SuppressWarnings("unchecked")
  public void setData(Map<String, Object> data) {
    if (data != null) {
      this.oldValue = (Map<String, Object>) data.get("old");
      this.newValue = (Map<String, Object>) data.get("new");
    }
  }
}
