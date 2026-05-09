//var bcrypt = dcodeIO.bcrypt;

layui.use(['form'], function(){
    var form = layui.form;
    var layer = layui.layer;
    // 提交事件
    form.on('submit(demo1)', function(data){
        var field = data.field; // 获取表单字段值
        //console.log(field);
        var name = Base64.encode(Base64.encode($("#name").val()));
        var pass = Base64.encode(Base64.encode($("#pass").val()));

        let commonStr = {};
        commonStr.errorInfo="error!";

        $.ajax({
            type: 'POST',
            url: ctx + '/token/getToken',
            data: {
                name : name,
                pass : pass
            },
            dataType: 'json',
            success: function(data) {
                if (data.code == 200) {
                    Cookies.set(COOK_NAME.token, data.data, { expires: 1, path: '/', SameSite: "Strict" })
                    location.href = ctx + "/admin";
                } else {
                    layer.msg(data.description);
                    //refreshCode('codeImg');
                }
            },
            error: function() {
                layer.alert(commonStr.errorInfo);
            }
        });
        return false;
    });

//    $('#pass').on('keydown', function (event) {
//        if (event.keyCode == 13) {
//            $("#loginBtn").trigger("click");
//            return false
//        }
//    });



});
