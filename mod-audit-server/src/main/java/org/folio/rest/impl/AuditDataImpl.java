package org.folio.rest.impl;

import java.util.Map;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Audit;
import org.folio.rest.jaxrs.model.AuditCollection;
import org.folio.rest.jaxrs.resource.AuditData;
import org.folio.rest.persist.PgUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class AuditDataImpl implements AuditData {

  private static final Logger LOGGER = LogManager.getLogger();

  protected static final String API_CXT = "/audit-data";
  protected static final String DB_TAB_AUDIT = "audit_data";

  @Override
  @Validate
  public void getAuditData(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    LOGGER.debug("getAuditData:: Retrieving audit data with query: {}", query);
    PgUtil.get(DB_TAB_AUDIT, Audit.class, AuditCollection.class, query, offset, limit, okapiHeaders, vertxContext,
        GetAuditDataResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void postAuditData(String lang, Audit audit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    LOGGER.debug("postAuditData:: Creating new audit data with id: {}", audit.getId());
    PgUtil.post(DB_TAB_AUDIT, audit, okapiHeaders, vertxContext, PostAuditDataResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void getAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    LOGGER.debug("getAuditDataById:: Retrieving audit data by id: {}", id);
    PgUtil.getById(DB_TAB_AUDIT, Audit.class, id, okapiHeaders, vertxContext, GetAuditDataByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void putAuditDataById(String id, String lang, Audit audit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    LOGGER.debug("putAuditDataById:: Updating audit data by id: {}", id);
    PgUtil.put(DB_TAB_AUDIT, audit, id, okapiHeaders, vertxContext, PutAuditDataByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    LOGGER.debug("deleteAuditDataById:: Deleting audit data by id: {}", id);
    PgUtil.deleteById(DB_TAB_AUDIT, id, okapiHeaders, vertxContext, DeleteAuditDataByIdResponse.class, asyncResultHandler);
  }
}
