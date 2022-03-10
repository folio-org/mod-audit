package org.folio.rest.impl;

import java.util.Map;
import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Audit;
import org.folio.rest.jaxrs.model.AuditCollection;
import org.folio.rest.jaxrs.resource.AuditData;
import org.folio.rest.persist.PgUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class AuditDataImpl implements AuditData {

  protected static final String API_CXT = "/audit-data";
  protected static final String DB_TAB_AUDIT = "audit_data";

  @Override
  @Validate
  public void getAuditData(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    PgUtil.get(DB_TAB_AUDIT, Audit.class, AuditCollection.class, query, offset, limit, okapiHeaders, vertxContext,
        GetAuditDataResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void postAuditData(String lang, Audit audit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    PgUtil.post(DB_TAB_AUDIT, audit, okapiHeaders, vertxContext, PostAuditDataResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void getAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    PgUtil.getById(DB_TAB_AUDIT, Audit.class, id, okapiHeaders, vertxContext, GetAuditDataByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void putAuditDataById(String id, String lang, Audit audit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    PgUtil.put(DB_TAB_AUDIT, audit, id, okapiHeaders, vertxContext, PutAuditDataByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    PgUtil.deleteById(DB_TAB_AUDIT, id, okapiHeaders, vertxContext, DeleteAuditDataByIdResponse.class, asyncResultHandler);
  }
}
