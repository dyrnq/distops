var editors = {};

function copyEditorContent(editorId) {
    var text = '';
    if (editors[editorId]) {
        text = editors[editorId].getValue();
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(function() { layer.msg('Copied'); });
    } else {
        var ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.left = '-9999px';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        layer.msg('Copied');
    }
}

function getAceMode(key) {
    if (key === 'config.yaml' || key === 'config.yml') return 'yaml';
    if (key === 'supervisor' || key === 'registry.ini') return 'ini';
    if (key === 'jwks.json') return 'json';
    if (key === 'htpasswd') return 'plain_text';
    return 'plain_text';
}

function initEditor(containerId, mode, content) {
    var editor = ace.edit(containerId);
    editor.setTheme("ace/theme/twilight");
    editor.session.setMode("ace/mode/" + mode);
    editor.session.setOption("tabSize", 2);
    editor.setFontSize(14);
    editor.setOptions({ minLines: 20, maxLines: Infinity });
    editor.setReadOnly(true);
    editor.setValue(content || '', -1);
    editors[containerId] = editor;
    return editor;
}

layui.use(function() {
    var element = layui.element;

    if (!id) return;

    $.ajax({
        url: ctx + '/api/inst/config',
        type: 'get',
        data: { id: id },
        success: function(data) {
            if (data.code != '200') return;

            var keys = Object.keys(data.data);
            if (keys.length === 0) return;

            var headerHtml = '';
            var bodyHtml = '';

            for (var i = 0; i < keys.length; i++) {
                var key = keys[i];
                var config = data.data[key];
                var editorId = 'editor-' + i;
                var mode = getAceMode(key);

                headerHtml += '<li lay-id="' + key + '"' + (i === 0 ? ' class="layui-this"' : '') + '>' + key + '</li>';
                bodyHtml += '<div class="layui-tabs-item' + (i === 0 ? ' layui-show' : '') + '">';
                bodyHtml += '<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">';
                bodyHtml += '<span style="color: #999;">' + (config.path || '') + '</span>';
                bodyHtml += '<button type="button" class="layui-btn layui-btn-xs layui-btn-normal" onclick="copyEditorContent(\'' + editorId + '\')">Copy</button>';
                bodyHtml += '</div>';
                bodyHtml += '<div id="' + editorId + '" class="ace-editor-tab"></div>';
                bodyHtml += '</div>';
            }

            $('#tabHeader').html(headerHtml);
            $('#tabBody').html(bodyHtml);

            setTimeout(function() {
                for (var i = 0; i < keys.length; i++) {
                    var config = data.data[keys[i]];
                    var editorId = 'editor-' + i;
                    var mode = getAceMode(keys[i]);
                    initEditor(editorId, mode, config.body || '');
                }
                if (editors['editor-0']) editors['editor-0'].resize();
            }, 200);

            element.on('tab(configTabs)', function(data) {
                var layId = data.elem.getAttribute('lay-id');
                var idx = keys.indexOf(layId);
                if (idx >= 0) {
                    setTimeout(function() {
                        if (editors['editor-' + idx]) editors['editor-' + idx].resize();
                    }, 100);
                }
            });
        }
    });
});
