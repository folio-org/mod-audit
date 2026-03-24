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
}
