package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Represents a record containing bibliographic or authority data.
 * <p>
 * The Record class is designed to capture and represent the structure of
 * records used in library systems. It includes identifiers, the type of the record,
 * the parsed record content, and associated metadata.
 * <p>
 * Fields:
 * - matchedId: A unique identifier for the record.
 * - recordType: An enumeration indicating the type of the record (e.g., MARC_BIB, MARC_AUTHORITY).
 * - parsedRecord: A map containing the parsed content of the record.
 * - metadata: A map containing additional metadata related to the record.
 */
@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Record {
  private String matchedId;
  private Map<String, Object> parsedRecord;
  private Map<String, Object> metadata;
}
