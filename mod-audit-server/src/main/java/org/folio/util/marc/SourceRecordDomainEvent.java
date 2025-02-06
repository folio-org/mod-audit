package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents a domain event related to source records in a library system.
 * <p>
 * The SourceRecordDomainEvent class encapsulates the details of events that are
 * triggered in response to changes in source records. This includes information
 * about the event's unique identifier, type, associated metadata, and payload.
 * <p>
 * Fields:
 * - eventId: A unique identifier for the domain event.
 * - eventType: The type of the event, indicating the action performed (created, updated, deleted).
 * - eventMetadata: Metadata associated with the event, such as publisher, tenant information,
 *   and the event's timestamp.
 * - eventPayload: The payload of the event, containing details of the new and old states of the record.
 */
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
}
