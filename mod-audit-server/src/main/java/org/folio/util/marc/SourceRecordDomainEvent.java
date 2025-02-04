package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceRecordDomainEvent {
  private static final String ID = "id";

  @JsonProperty("id")
  private String eventId;
  private SourceRecordDomainEventType eventType;
  private SourceRecordType recordType;
  private EventMetadata eventMetadata;
  private EventPayload eventPayload;
  private String recordId;

  public void setEventPayload(EventPayload eventPayload) {
    this.eventPayload = eventPayload;
    this.recordId = computeRecordId(eventPayload);
  }

  private static String computeRecordId(EventPayload eventPayload) {
    if (eventPayload != null) {
      if (eventPayload.getNewRecord() != null && !eventPayload.getNewRecord().isEmpty()) {
        return eventPayload.getNewRecord().get(ID).toString();
      } else if (eventPayload.getOld() != null) {
        return eventPayload.getOld().get(ID).toString();
      }
    }
    return null;
  }

}
