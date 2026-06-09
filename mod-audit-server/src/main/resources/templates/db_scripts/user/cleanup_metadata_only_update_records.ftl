<#if mode.name() == "UPDATE">

DELETE FROM ${myuniversity}_${mymodule}.user_audit
WHERE action = 'UPDATED'
  AND NOT EXISTS (
    SELECT 1 FROM jsonb_array_elements(
      CASE WHEN jsonb_typeof(diff->'fieldChanges') = 'array'
           THEN diff->'fieldChanges' ELSE '[]'::jsonb END
    ) AS fc WHERE NOT (fc->>'fullPath' = ANY(ARRAY[
      'createdDate', 'updatedDate',
      'metadata.createdDate', 'metadata.updatedDate',
      'metadata.createdByUserId', 'metadata.updatedByUserId'
    ]))
  )
  AND NOT EXISTS (
    SELECT 1 FROM jsonb_array_elements(
      CASE WHEN jsonb_typeof(diff->'collectionChanges') = 'array'
           THEN diff->'collectionChanges' ELSE '[]'::jsonb END
    ) AS cc WHERE NOT (cc->>'fullPath' = ANY(ARRAY[
      'createdDate', 'updatedDate',
      'metadata.createdDate', 'metadata.updatedDate',
      'metadata.createdByUserId', 'metadata.updatedByUserId'
    ]))
  );

</#if>
