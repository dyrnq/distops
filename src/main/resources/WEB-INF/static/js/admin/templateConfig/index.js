var editorYaml = null;
var editorIni = null;

function initAceEditor(elementId, mode) {
    var editor = ace.edit(elementId);
    editor.setTheme("ace/theme/twilight");
    editor.session.setMode("ace/mode/" + mode);
    editor.session.setOption("tabSize", 2);
    editor.setFontSize(16);
    editor.setOptions({
        minLines: 20,
        maxLines: Infinity
    });
    editor.resize();
    return editor;
}

layui.use(function() {

    var layer = layui.layer;
    var element = layui.element;

    // init both ACE editors
    editorYaml = initAceEditor("editorYaml", "yaml");
    editorIni = initAceEditor("editorIni", "ini");

    // resize editors when tab switches
    element.on('tab(templateTabs)', function(data) {
        var layId = data.elem.getAttribute('lay-id');
        setTimeout(function() {
            if (layId === 'yaml' && editorYaml) {
                editorYaml.resize();
            } else if (layId === 'ini' && editorIni) {
                editorIni.resize();
            }
        }, 100);
    });

    // load template data
    $.ajax({
        url: ctx + '/api/globalConfig/getTemplates',
        type: 'get',
        dataType: 'json',
        success: function(res) {
            if (res.code == '200') {
                if (res.data.yaml && editorYaml) {
                    editorYaml.setValue(res.data.yaml, -1);
                    editorYaml.clearSelection();
                }
                if (res.data.ini && editorIni) {
                    editorIni.setValue(res.data.ini, -1);
                    editorIni.clearSelection();
                }
            } else {
                layer.msg(res.description);
            }
        },
        error: function() {
            layer.alert(commonStr.errorInfo);
        }
    });

    // save button
    $('#saveBtn').click(function() {

        var yamlValue = editorYaml ? editorYaml.getValue() : '';
        var iniValue = editorIni ? editorIni.getValue() : '';

        var yamlSaved = false;
        var iniSaved = false;
        var hasError = false;

        function checkDone() {
            if (yamlSaved && iniSaved) {
                layer.msg(commonStr.success);
            }
        }

        // save YAML template
        $.ajax({
            url: ctx + '/api/globalConfig/saveTemplate',
            type: 'post',
            data: {
                name: 'registry_config_yml_template',
                value: yamlValue
            },
            dataType: 'json',
            success: function(res) {
                if (res.code == '200') {
                    yamlSaved = true;
                    checkDone();
                } else {
                    if (!hasError) {
                        hasError = true;
                        layer.msg(res.description);
                    }
                }
            },
            error: function() {
                if (!hasError) {
                    hasError = true;
                    layer.alert(commonStr.errorInfo);
                }
            }
        });

        // save INI template
        $.ajax({
            url: ctx + '/api/globalConfig/saveTemplate',
            type: 'post',
            data: {
                name: 'registry_supervisor_template',
                value: iniValue
            },
            dataType: 'json',
            success: function(res) {
                if (res.code == '200') {
                    iniSaved = true;
                    checkDone();
                } else {
                    if (!hasError) {
                        hasError = true;
                        layer.msg(res.description);
                    }
                }
            },
            error: function() {
                if (!hasError) {
                    hasError = true;
                    layer.alert(commonStr.errorInfo);
                }
            }
        });
    });

});
