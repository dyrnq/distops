function cleanData(d){
    console.log(d)
    $('#addForm1 input, #addForm1 select, #addForm1 textarea, #addForm1 checkbox').val('');
    if( d === true ){
        $("#div_id").hide();
        $('#addForm1 input[name="u"]').val("update");
    }else{
        $('#addForm1 input[name="u"]').val("add");
        $("#div_id").show();
    }
    layui.form.render('select');
    layui.form.render('checkbox');

//    console.log($('#addForm1 input[name="autoJob"]').val())
    $('#addForm1 input[name="autoJob"]').prop('checked', true);

}




layui.use(function() {

    var layer = layui.layer;
    var laypage = layui.laypage;
    var table = layui.table;
    var form = layui.form;
    var upload = layui.upload;
    var element = layui.element;

    var default_limit = localStorage.getItem('pageLimit');
    if ('' == default_limit || null == default_limit || undefined == default_limit) {
        default_limit = cfg.pageLimit;
    }

    form.on('switch(demo-checkbox-filter)', function(data){
        var elem = data.elem;
        var checked = elem.checked;
        var value = elem.value;
        var othis = data.othis;
        if(checked){
            $(elem).val("1");
        }else{
            $(elem).val("0");
        }
       layui.form.render('checkbox');
    });

    // resize ACE editor when yaml tab (eee) becomes visible
    element.on('tab(demoTabs1)', function(data){
        if (data.elem.getAttribute('lay-id') === 'eee') {
            if (typeof editor !== 'undefined' && editor) {
                setTimeout(function(){ editor.resize(); }, 100);
            }
        }
    });

    cleanData(id ? true : false);

    if (id) {
        $.ajax({
            url: ctx + '/api/inst/get',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify({id: id}),
            success: function (data, statusText) {
                if (data.code == '200') {
                    $('#addForm1 input, #addForm1 select, #addForm1 textarea, #addForm1 checkbox').each(function () {
                        var elementName = $(this).attr('name');
                        if (elementName in data.data) {
                            var val = data.data[elementName];
                            if (elementName === 'env' && val) {
                                // convert comma-separated to newline for textarea
                                val = val.replace(/,/g, '\n');
                            }
                            $(this).val(val);
                        }
                        console.log(elementName)
                        if (elementName === 'enabled') {
                            if (data.data[elementName] === 1) {
                                $(this).prop('checked', true);
                            } else {
                                $(this).prop('checked', false);
                            }
                        }
                    });
                    form.render('select');
                    form.render('checkbox');
                    form.render();

                    // load extraYaml into ACE editor
                    if (typeof editor !== 'undefined' && editor) {
                        var extraYamlVal = data.data.extraYaml || data.data.extra_yaml || '';
                        editor.setValue(extraYamlVal, -1);
                        editor.clearSelection();
                    }

                } else {
                    layer.msg(data.description);
                }
            },
            'error': function () {
                layer.msg(commonStr.errorInfo);
            }
        });
    } else {
        form.render('select');
        form.render('checkbox');
        form.render();
    }

    $('#addOver').click(function(){
        // sync ACE editor content to hidden input before serialize
        if (typeof editor !== 'undefined' && editor) {
            $('#extraYamlInput').val(editor.getValue());
        }

        var formData = $('#addForm1').serializeArray();

        // convert env newlines to commas before submit
        $.each(formData, function() {
            if (this.name === 'env' && this.value) {
                this.value = this.value.replace(/\n/g, ',');
            }
        });

        var serializedData = formData.reduce(function(acc, curr) {
            return acc + (acc ? '&' : '') + curr.name + '=' + encodeURIComponent(curr.value);
        }, '');

        var url = id ? ctx + '/api/inst/update' : ctx + '/api/inst/add';

        $.ajax({
            type : 'POST',
            url: url,
            data : serializedData,
            dataType : 'json',
            success : function(data) {
                if(data.code=='200'){
                    layer.msg(commonStr.success);
                    // close the iframe/layer and go back
                    setTimeout(function(){
                        var index = parent.layer.getFrameIndex(window.name);
                        parent.layer.close(index);
                        parent.layui.table.reload('demo',{});
                    }, 1000);
                } else {
                    layer.msg(data.description);
                }
            },
            error : function() {
                layer.alert(commonStr.errorInfo);
            }
        });
    });

});