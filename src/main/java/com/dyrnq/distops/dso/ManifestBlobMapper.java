package com.dyrnq.distops.dso;

import com.dyrnq.distops.model.ManifestBlob;
import org.noear.solon.annotation.Component;
import org.noear.wood.mapper.BaseMapperWrap;

@Component
public class ManifestBlobMapper extends BaseMapperWrap<ManifestBlob> {
    public ManifestBlobMapper() {
        super(null, ManifestBlob.class, ManifestBlob.TABLE_NAME);
    }
}
