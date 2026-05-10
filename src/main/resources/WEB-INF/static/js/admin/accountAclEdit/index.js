var aclEditor = null;

function initAclEditor() {
    aclEditor = ace.edit("editorAcl");
    aclEditor.setTheme("ace/theme/twilight");
    aclEditor.session.setMode("ace/mode/json");
    aclEditor.session.setOption("tabSize", 2);
    aclEditor.setFontSize(16);
    aclEditor.setOptions({
        minLines: 20,
        maxLines: Infinity
    });
    aclEditor.resize();
}

layui.use(function() {
    var layer = layui.layer;

    initAclEditor();

    if (id) {
        $.ajax({
            url: ctx + '/api/account/acl/get',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify({ id: id }),
            success: function(data) {
                if (data.code == '200') {
                    $('#acl_username').val(data.data.username);
                    if (aclEditor) {
                        aclEditor.setValue(data.data.acl || '', -1);
                        aclEditor.clearSelection();
                    }
                } else {
                    layer.msg(data.description);
                }
            },
            error: function() {
                layer.msg(commonStr.errorInfo);
            }
        });
    }

    $('#saveBtn').click(function() {
        var acl = '';
        if (aclEditor) {
            acl = aclEditor.getValue();
        }

        // Validate JSON
        if (acl && acl.trim()) {
            try {
                JSON.parse(acl);
            } catch (e) {
                layer.msg('Invalid JSON format: ' + e.message);
                return;
            }
        }

        $.ajax({
            url: ctx + '/api/account/acl/update',
            type: 'post',
            data: { id: id, acl: acl },
            dataType: 'json',
            success: function(data) {
                if (data.code == '200') {
                    layer.msg(commonStr.success);
                    setTimeout(function() {
                        var index = parent.layer.getFrameIndex(window.name);
                        parent.layer.close(index);
                        parent.layui.table.reload('demo', {});
                    }, 1000);
                } else {
                    layer.msg(data.description);
                }
            },
            error: function() {
                layer.alert(commonStr.errorInfo);
            }
        });
    });
});
