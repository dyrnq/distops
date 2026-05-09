package ${mapper_pkg};

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
<#list imports as item>
import ${item};
</#list>
import ${full_domain};
import ${package_name}.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class ${domain}Mapper extends BaseMapperWrap<${domain}> {

    public ${domain}Mapper() {
        super(null, ${domain}.class, ${domain}.TABLE_NAME);
    }

${autowired!}
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



${customize_begin!}
${customize_content!}
${customize_end!}
}
