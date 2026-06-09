package org.folio.dao.user;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserAuditConstants {

  /**
   * Field paths removed during anonymization — both in Java (UserEventServiceImpl)
   * and in SQL (UserEventDaoImpl.ANONYMIZE_ALL_SQL).
   */
  public static final Set<String> ANONYMIZED_FIELD_PATHS =
    Set.of("metadata.createdByUserId", "metadata.updatedByUserId");

  // events containing only these paths are discarded as noise if there are no other changes
  public static final Set<String> INTERNAL_METADATA_FIELD_PATHS = Set.of(
    "createdDate", "updatedDate",
    "metadata.createdDate", "metadata.updatedDate",
    "metadata.createdByUserId", "metadata.updatedByUserId"
  );
}
