
package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * The EventMetadata class encapsulates metadata related to an event.
 * It provides information such as the publisher of the event, the tenant
 * associated with the event, and the timestamp of the event.
 */
@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventMetadata {
    private String publishedBy;
    private String tenantId;
    private LocalDateTime eventDate;
}
