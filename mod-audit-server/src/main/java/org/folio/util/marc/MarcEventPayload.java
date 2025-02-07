
package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents the payload of an event containing both the new and old record states.
 * <p>
 * This class encapsulates the data for an event's records before and after the change,
 * allowing the tracking of transformations or updates. The "new" record denotes
 * the updated state, while "old" represents the previous state of the record.
 */
@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcEventPayload {
    @JsonProperty("new")
    private Record newRecord;
    private Record old;
}
