package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;


/**
 * Represents a domain event related to a SourceRecord in a library cataloging system.
 * <p>
 * The SourceRecordDomainEvent class encapsulates information about a specific type of event
 * associated with a MARC record, including its metadata, payload, and type. This class is
 * utilized to track changes or actions performed on a MARC record, such as its creation,
 * update, or deletion.
 * <p>
 * Fields:
 * - eventId: A unique identifier representing the event.
 * - recordType: Represents the type of source record (e.g., bibliographic or authority)
 *   associated with the event. This is defined by the SourceRecordType enum.
 * - eventMetadata: Contains metadata related to the event, such as publisher details,
 *   event timestamp, and tenant information.
 * - eventPayload: Contains details about the state of the record involved in the event,
 *   including its previous and updated state. This is defined by the MarcEventPayload class.
 * - eventType: Specifies the type of the domain event, such as creation, update, deletion,
 *   or other states. This is defined by the SourceRecordDomainEventType enum.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceRecordDomainEvent {
  @JsonProperty("id")
  private String eventId;
  private SourceRecordType recordType;
  private EventMetadata eventMetadata;
  private MarcEventPayload eventPayload;
  private SourceRecordDomainEventType eventType;
}
