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
  @JsonProperty("id")
  private String eventId;
  private SourceRecordDomainEventType eventType;
  private EventMetadata eventMetadata;
  private EventPayload eventPayload;
  private String recordId;
}
