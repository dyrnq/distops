function cleanData(d){
    if( d === true ){
        $("#div_id").hide();
        $("#div_pass").hide();
        $('#addForm1 input[name="u"]').val("update");
    }else{
        $("#div_id").show();
        $("#div_pass").show();
        $('#addForm1 input[name="u"]').val("add");
    }

    $('#addForm1 input[name="id"]').val("");
    $('#addForm1 input[name="name"]').val("");
    $('#addForm1 input[name="pass"]').val("");
    $('#addForm1 input[name="email"]').val("");
    $('#addForm1 input[name="phone"]').val("");
}


function addLink(d) {
    var addLink = d.id;
    if ('' == addLink || null == addLink || undefined == addLink) {
        return '';
    }
   if (addLink.length > 0) {
       let editBtn = '<button type="button" class="layui-btn layui-btn-normal layui-btn-xs" lay-event="edit">' + commonStr.edit + '</button>'
       let changePassBtn = '<button type="button" class="layui-btn layui-btn-normal layui-btn-xs" lay-event="changePass">改密</button>'
       let delBtn  = '<button type="button" class="layui-btn layui-btn-danger layui-btn-xs" lay-event="del">'+commonStr.del+'</button>'
       return editBtn+'&nbsp;'+changePassBtn+'&nbsp;'+delBtn;
   }
}

layui.use(function(){

var layer = layui.layer;
var laypage = layui.laypage;
var table = layui.table;
var form = layui.form;


$('#change').click(function () {

    let newPass = Base64.encode(Base64.encode($("#newPass").val()));
    let confirmPass = Base64.encode(Base64.encode($("#confirmPass").val()));
    let id = $('#addForm2 input[name="id"]').val();
    if (newPass != confirmPass) {
        layer.msg(loginStr.error3)
    } else {
        $.ajax({
            url: ctx + '/api/user/changePass',
            type: 'post',
            contentType: 'application/json',
            data: JSON.stringify({id: id, newPass: newPass}),
            success: function (data, statusText) {
                if (data.code == '200') {
                    layer.closeAll();
                    layer.msg(commonStr.success);
                } else {
                    layer.msg(data.description);
                }
            }
        });
    }
});

var default_limit = localStorage.getItem('pageLimit');

if ('' == default_limit || null == default_limit || undefined == default_limit) {
    default_limit = cfg.pageLimit;
}

$('#add').click(function(){
    cleanData(false);
    layer.open({
        type: 1,
        area: ['800px', '600px'],
        title: 'Add',
        content : $('#windowDiv'),
        anim: 'slideRight',
        shade: 0.6, // 遮罩透明度
        shadeClose: true, // 点击遮罩区域，关闭弹层
        maxmin: true, // 允许全屏最小化
        skin: 'layui-layer-win10'
    });

});


$('#addOver').click(function(){
    let u = $('#addForm1 input[name="u"]').val();
    var formData = $("#addForm1").serializeArray().reduce(function(obj, item) {
      obj[item.name] = item.value;
      return obj;
    }, {});

    if ( 'add' == u ){
        formData['pass']= Base64.encode(Base64.encode($('#addForm1 input[name="pass"]').val()));
    }else{
        formData['pass'] = null;
    }
    //console.log(formData);
    $.ajax({
        type : 'POST',
        url: ctx + '/api/user/'+u,
        data : JSON.stringify(formData),
        dataType : 'json',
        contentType: 'application/json',
        success : function(data) {
            if(data.code=='200'){
                layer.closeAll();
                layer.msg(commonStr.success);
                table.reload('demo',{});
            } else {
                layer.msg(data.description);
            }
        },
        error : function() {
            layer.alert(commonStr.errorInfo);
        }
    });
});


    //执行一个 table 实例
    table.render({
        elem: '#demo'
        ,height: 620
        ,url: ctx + '/api/user' //数据接口
        ,title: '用户表'
        ,page: true //开启分页
        , limit: default_limit
        , limits: cfg.pageLimits
        ,toolbar: '#toolbarDemo' //开启工具栏，此处显示默认图标，可以自定义模板，详见文档
        , defaultToolbar: ['filter', 'exports', 'print', { //自定义头部工具栏右侧图标。如无需自定义，去除该参数即可
            title: '提示'
            , layEvent: 'LAYTABLE_TIPS'
            , icon: 'layui-icon-tips'
        }]
        ,totalRow: false //开启合计行
        ,cols: [[ //表头
            {type: 'checkbox', fixed: 'left'}
            ,{field: 'id', title: 'id', width: 100, sort: true, fixed: 'left', totalRowText: '合计：'}
            ,{field: 'name', title: 'name', width: 80}
            ,{field: 'email', title: 'email', width: 200}
            ,{field: 'phone', title: 'phone', width: 200}
            ,{field: 'upstream', title: 'operation', fixed: 'right',  templet: addLink}
        ]]
        , done: function (res, curr, count){
            //如果是异步请求数据方式，res即为你接口返回的信息。
            //如果是直接赋值的方式，res即为：{data: [], count: 99} data为当前页数据、count为数据总长度
            //console.log(res);
            //得到当前页码
            //console.log(curr);
            //得到数据总量
            //console.log(count);


            // 获取配置项
            var thisOptions = table.getOptions('demo');
            //console.log(thisOptions);
            localStorage.setItem('pageLimit', thisOptions.limit);


            if(res.data && res.data.length == 0){
                if(curr>1){
                    toPage=curr-1;
                    //console.log(toPage);
                    table.reload('demo',{page: {curr:toPage}});
                }
            }
        }
        ,response: {
            statusCode: 200
        }
        ,parseData: function(res){ //将原始数据解析成 table 组件所规定的数据
            return {
                "code": res.code, //解析接口状态
                "msg": res.description, //解析提示文本
                "count": res.total, //解析数据长度
                "data": res.data //解析数据列表
            };
        }
    });

    //头部工具条事件
    table.on('toolbar(test)', function (obj) {
        var checkStatus = table.checkStatus(obj.config.id);
        switch (obj.event) {
            case 'deleteSelected':
                var dataX = checkStatus.data;
                var allId = [];
                if (dataX.length === 0) {
                    layer.msg(commonStr.pleaseSelect);
                } else {
                    layer.confirm(commonStr.confirmBatchDelete, function(index) {
                        for (let i = 0; i < dataX.length; i++) {
                            const val = dataX[i];
                            if(val.id == "1"){
                                continue;//在id=1的情况下跳过剩余循环，直接进入id=2的循环。
                            }else{
                                allId.push(val.id)
                            }
                        }
                        $.ajax({
                            url: ctx + '/api/user/del',
                            type:'post',
                            contentType: 'application/json',
                            data: JSON.stringify({id: allId}),
                            success:function (data,statusText) {

                                if(data.code=='200'){
                                    table.reload('demo',{});
                                    layer.msg(commonStr.delSuccess);

                                }else{
                                    layer.msg(data.description);
                                }
                            },
                            'error':function () {
                                layer.msg(commonStr.errorInfo);
                            }
                        });


                        layer.close(index);
                    });


                }
                break;
            case 'add':
                cleanData(false);
                layer.open({
                    type: 1,
                    area: ['800px', '600px'],
                    title: 'Add',
                    content : $('#windowDiv'),
                    anim: 'slideRight',
                    shade: 0.6, // 遮罩透明度
                    shadeClose: true, // 点击遮罩区域，关闭弹层
                    maxmin: true, // 允许全屏最小化
                    skin: 'layui-layer-win10'
                });
                break;
        }
    })

    //工具条事件
    table.on('tool(test)', function(obj){ //注：tool 是工具条事件名，test 是 table 原始容器的属性 lay-filter="对应的值"
        var data = obj.data; //获得当前行数据
        var layEvent = obj.event; //获得 lay-event 对应的值（也可以是表头的 event 参数对应的值）
        var tr = obj.tr; //获得当前行 tr 的 DOM 对象（如果有的话）

        if(layEvent === 'detail'){ //查看

        } else if(layEvent === 'del'){ //删除

            if(obj.data.id == "1") {
                layer.msg(commonStr.cantDel)
            }else{
                layer.confirm(commonStr.confirmDel, function (index) {
                    obj.del();
                    $.ajax({
                        url: ctx + '/api/user/del',
                        type: 'post',
                        contentType: 'application/json',
                        data: JSON.stringify({id: [obj.data.id] }),
                        success: function (data, statusText) {

                            if (data.code == '200') {
                                layer.msg(commonStr.delSuccess);
                                table.reload("demo")

                            } else {
                                layer.msg(data.description);
                            }
                        },
                        'error': function () {
                            layer.msg(commonStr.errorInfo);
                        }
                    });

                    layer.close(index);
                });
            }
        } else if (layEvent === 'edit'){
            layer.open({
                type: 2,
                title: 'Edit User',
                shadeClose: true,
                shade: 0.8,
                area: ['500px', '400px'],
                content: ctx + '/admin/userEdit-' + obj.data.id,
                anim: 'slideRight',
                maxmin: true,
                skin: 'layui-layer-win10'
            });
        }else if(layEvent == 'changePass'){
            layer.open({
                type: 2,
                title: 'Change Password',
                shadeClose: true,
                shade: 0.8,
                area: ['450px', '300px'],
                content: ctx + '/admin/userPassEdit-' + obj.data.id,
                anim: 'slideRight',
                maxmin: true,
                skin: 'layui-layer-win10'
            });



        }
    });

    //监听Tab切换
    element.on('tab(demo)', function(data){
        layer.tips('切换了 '+ data.index +'：'+ this.innerHTML, this, {
            tips: 1
        });
    });
});