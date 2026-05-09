function cleanData(d) {
    console.log(d)
    $('#addForm1 input, #addForm1 select, #addForm1 textarea, #addForm1 checkbox').val('');
    if (d === true) {
        $("#div_id").hide();
        $('#addForm1 input[name="u"]').val("update");
    } else {
        $('#addForm1 input[name="u"]').val("add");
        $("#div_id").show();
    }
    layui.form.render('select');
    layui.form.render('checkbox');
}

layui.use(function() {

    var layer = layui.layer;
    var form = layui.form;

    form.on('switch(demo-checkbox-filter)', function(data) {
        var elem = data.elem;
        var checked = elem.checked;
        if (checked) {
            $(elem).val("1");
        } else {
            $(elem).val("0");
        }
        layui.form.render('checkbox');
    });

    cleanData(id ? true : false);

    if (id) {
        $.ajax({
            url: ctx + '/api/account/get',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify({ id: id }),
            success: function(data, statusText) {
                if (data.code == '200') {
                    $('#addForm1 input, #addForm1 select, #addForm1 textarea, #addForm1 checkbox').each(function() {
                        var elementName = $(this).attr('name');
                        if (elementName in data.data) {
                            $(this).val(data.data[elementName]);
                        }
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
                } else {
                    layer.msg(data.description);
                }
            },
            error: function() {
                layer.msg(commonStr.errorInfo);
            }
        });
    } else {
        form.render('select');
        form.render('checkbox');
        form.render();
    }

    $('#addOver').click(function() {
        var formData = $('#addForm1').serializeArray();
        var serializedData = formData.reduce(function(acc, curr) {
            return acc + (acc ? '&' : '') + curr.name + '=' + encodeURIComponent(curr.value);
        }, '');

        var url = id ? ctx + '/api/account/update' : ctx + '/api/account/add';

        $.ajax({
            type: 'POST',
            url: url,
            data: serializedData,
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
