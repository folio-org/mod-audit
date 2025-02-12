package org.folio.domain.diff;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRecordDto {
  private List<FieldChangeDto> fieldChanges;
  private List<CollectionChangeDto> collectionChanges;
}
