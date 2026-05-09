DROP VIEW IF EXISTS `${table!}_view`;
CREATE VIEW ${table!}_view AS
SELECT
<#list fieldList as item>
cl.${item.columnName!} as ${item.columnName!},
</#list>
cg.group_name AS group_name,
cg.group_code AS group_code,
cg.member_count AS group_member_count,
cg.status AS group_status,
cg.create_time AS group_create_time,
cg.update_time AS group_update_time
FROM
  ${table!} cl
  LEFT JOIN chat_group cg ON cl.group_id = cg.id;


