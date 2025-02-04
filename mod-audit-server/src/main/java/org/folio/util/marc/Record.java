package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Record {
  private String id;
  private SourceRecordType recordType;
  private Map<String, Object> parsedRecord;
  private Map<String, Object> metadata;
}
