package ${package_name};

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
<#if openapi><#else>//</#if>import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("${table}")
@Data
//${tableComment!}
<#if openapi><#else>//</#if>@Schema(name = "${domain}", description = "${tableComment!}")
public class ${domain} implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="${table}";
<#list fieldList as item>
<#if item.pk>@PrimaryKey<#else></#if>
<#if item.fieldType == "Long">
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
<#else></#if>
@Column("${item.columnName!}")
<#if openapi><#else>//</#if>@Schema(description = "${item.columnComment!}")
// ${item.columnComment!}
// ${item.columnType!}
// ${item.columnLength!}
public ${item.fieldType!} ${item.fieldName!};

</#list>



<#list fieldList as item>
public static final String ${item.finalFieldName!}="${item.columnName!}";
</#list>

/** GEN layui column
<#list fieldList as item>
, {field: '${item.fieldName!}', title: '${item.fieldName!}'}
</#list>
**/

${customize_begin!}
${customize_content!}
${customize_end!}

}
