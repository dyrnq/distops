layui.use(function() {
    var layer = layui.layer;
    var form = layui.form;

    if (id) {
        $('#changeForm input[name="id"]').val(id);
        form.render();
    }

    $('#changeBtn').click(function() {
        var newPass = $('#newPass').val();
        var confirmPass = $('#confirmPass').val();
        var userId = $('#changeForm input[name="id"]').val();

        if (!newPass || !confirmPass) {
            layer.msg(loginStr.timeFill);
            return;
        }
        if (newPass !== confirmPass) {
            layer.msg(loginStr.error3);
            return;
        }

        var encodedPass = Base64.encode(Base64.encode(newPass));

        $.ajax({
            url: ctx + '/api/user/changePass',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify({ id: userId, newPass: encodedPass }),
            success: function(data) {
                if (data.code == '200') {
                    layer.msg(commonStr.success);
                    setTimeout(function() {
                        var index = parent.layer.getFrameIndex(window.name);
                        parent.layer.close(index);
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
