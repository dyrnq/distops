layui.use(function() {
    var layer = layui.layer;
    var form = layui.form;

    if (id) {
        $.ajax({
            url: ctx + '/api/user/get',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify({ id: id }),
            success: function(data) {
                if (data.code == '200') {
                    $('#addForm1 input[name="id"]').val(data.data.id);
                    $('#addForm1 input[name="name"]').val(data.data.name);
                    $('#addForm1 input[name="email"]').val(data.data.email);
                    $('#addForm1 input[name="phone"]').val(data.data.phone);
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
        $('#div_id').show();
    }

    $('#addOver').click(function() {
        var formData = $("#addForm1").serializeArray().reduce(function(obj, item) {
            obj[item.name] = item.value;
            return obj;
        }, {});

        var url = id ? ctx + '/api/user/update' : ctx + '/api/user/add';

        if (!id) {
            formData['pass'] = 'dGVzdA=='; // default base64 encoded password
        }

        $.ajax({
            type: 'POST',
            url: url,
            data: JSON.stringify(formData),
            dataType: 'json',
            contentType: 'application/json',
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
