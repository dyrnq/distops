package com.dyrnq.distops.dso;

import com.dyrnq.distops.model.Blob;
import org.noear.solon.annotation.Component;
import org.noear.wood.mapper.BaseMapperWrap;

@Component
public class BlobMapper extends BaseMapperWrap<Blob> {
    public BlobMapper() {
        super(null, Blob.class, Blob.TABLE_NAME);
    }

    public Blob findByInstIdAndDigest(Long instId, String digest) {
        return selectItem(m -> m.whereEq(Blob.INST_ID, instId).andEq(Blob.DIGEST, digest));
    }
}
