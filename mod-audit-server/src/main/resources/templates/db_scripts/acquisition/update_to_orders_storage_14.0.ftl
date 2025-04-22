<#if mode.name() == "UPDATE">

-- MODORDSTOR-448 - Change userLimit to string
UPDATE ${myuniversity}_${mymodule}.acquisition_order_line_log
SET modified_content_snapshot = jsonb_set(modified_content_snapshot, '{eresource, userLimit}',
  to_jsonb(modified_content_snapshot->'eresource'->>'userLimit'))
WHERE modified_content_snapshot #> '{eresource, userLimit}' IS NOT NULL;

-- MODORDERS-1269 - Remove alerts and reporting codes from order lines
UPDATE ${myuniversity}_${mymodule}.acquisition_order_line_log
SET modified_content_snapshot = modified_content_snapshot - 'alerts' - 'reportingCodes';

</#if>
