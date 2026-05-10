//var editor1 = ace.edit("editor1");
//editor1.setTheme("ace/theme/twilight");
//editor1.session.setMode("ace/mode/yaml");
//editor1.setFontSize(16);
//editor1.setOptions({
//    minLines: 20,
//    maxLines: Infinity
//});
//editor1.resize();

function cleanData(d){
    console.log(d)
    $('#addForm1 input, #addForm1 select, #addForm1 textarea, #addForm1 checkbox').val('');
    if( d === true ){
        $("#div_id").hide();
        $("#div_instId").hide();
        $('#addForm1 input[name="u"]').val("update");
    }else{
        $('#addForm1 input[name="u"]').val("add");
        $("#div_id").show();
        $("#div_instId").show();
    }
    layui.form.render('select');
    layui.form.render('checkbox');

//    console.log($('#addForm1 input[name="autoJob"]').val())
    $('#addForm1 input[name="autoJob"]').prop('checked', true);

}

function addLink(d) {
    var addLink = d.id;
    console.log(d)
    console.log(typeof d.id)
    console.log(externalUrl)
//    var link = d.url.split('://')[1];
    let editBtn = '<button type="button" class="layui-btn layui-btn-normal layui-btn-xs" lay-event="edit">' + commonStr.edit + '</button>'
    let delBtn  = '<button type="button" class="layui-btn layui-btn-danger layui-btn-xs" lay-event="del">' + commonStr.del + '</button>'
    let aclBtn = '<button type="button" class="layui-btn layui-btn-warm layui-btn-xs" lay-event="acl">ACL</button>'

//    let url  = '<a class="layui-btn layui-btn-normal layui-btn-xs" href="'+externalUrl+''+link+'" target="_blank">link</a>'


    return [editBtn, aclBtn, delBtn].join("&nbsp;");
}
function addLog(d) {
    if (typeof d !== 'undefined' && d !== null && typeof d.currentJobId !== 'undefined' && d.currentJobId!==null) {
        let logBtn  = '<a class="layui-btn layui-btn-normal layui-btn-xs" href="'+ctx+'/api/artJob/log/'+d.currentJobId+'" target="_blank">log</a>'
        return logBtn;
    }else{
        return '';
    }
}

function add_finalStatus(d) {
    if (typeof d !== 'undefined' && d !== null && typeof d.finalStatus !== 'undefined' && d.finalStatus!==null) {
        if (d.finalStatus == 1) {
            return '完成';
        }else if(d.finalStatus == 2) {
            return '异常';
        }else {
            return d.finalStatus;
        }
    }else{
        return '';
    }
}


layui.use(function(){

var layer = layui.layer;
var laypage = layui.laypage;
var table = layui.table;
var form = layui.form;
var upload = layui.upload;

var default_limit = localStorage.getItem('pageLimit');
if ('' == default_limit || null == default_limit || undefined == default_limit) {
    default_limit = cfg.pageLimit;
}



layui.code({
    elem: '.code-demo',
    wordWrap: false,
    layout: ['code'],
    ln: false,
    lang: 'yaml',
    preview: false,
    header: true,
    text: {
      code: '多行文本,每行一条数据,格式为: <b>name,url,autoJob</b>,如果只有一列则为<b>url<b/>', // 默认:  </>
      preview: '预览栏标题' // 默认: Preview
    }
});

form.on('switch(demo-checkbox-filter)', function(data){
    var elem = data.elem; // 获得 checkbox 原始 DOM 对象
    var checked = elem.checked; // 获得 checkbox 选中状态
    var value = elem.value; // 获得 checkbox 值
    var othis = data.othis; // 获得 checkbox 元素被替换后的 jQuery 对象
    if(checked){
        $(elem).val("1");
    }else{
        $(elem).val("0");
    }
//    console.log('#########autoJob checked 状态: '+ elem.checked);
//    console.log('#########autoJob 值: '+ $(elem).val());
    layui.form.render('checkbox');
});

 form.on('submit(demo-table-search)', function(data){
     var field = data.field; // 获得表单字段
     // 执行搜索重载
     table.reload('demo', {
       page: {
         curr: 1 // 重新从第 1 页开始
       },
       where: field // 搜索的字段
     });
     return false; // 阻止默认 form 跳转
   });

$('#add').click(function(){
    layer.open({
        type: 2,
        title: 'Add Account',
        shadeClose: true,
        shade: 0.8,
        area: ['500px', '450px'],
        content: ctx + '/admin/accountEdit',
        anim: 'slideRight',
        maxmin: true,
        skin: 'layui-layer-win10'
    });
});





$('#addOver3').click(function(){

    var formData = $('#addForm3').serialize();
    $.ajax({
        type : 'POST',
        url: ctx + '/api/account/batchAdd',
        data : formData,
        dataType : 'json',
        success : function(data) {
            if(data.code=='200'){
                layer.closeAll();
                layer.msg(data.data);
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
        , height: 'full-30'
        , even: true
        , url: ctx + '/api/account' //数据接口
        , title: '用户表'
        , page: true //开启分页
        , limit: default_limit
        , limits: cfg.pageLimits
        , toolbar: '#toolbarDemo' //开启工具栏，此处显示默认图标，可以自定义模板，详见文档
        , defaultToolbar: ['filter', 'exports', 'print', { //自定义头部工具栏右侧图标。如无需自定义，去除该参数即可
            title: '提示'
            , layEvent: 'LAYTABLE_TIPS'
            , icon: 'layui-icon-tips'
        }]
        , totalRow: false //开启合计行
        , cols: [[ //表头
            {type: 'checkbox', fixed: 'left'}
            , {field: 'id', title: 'id', width: 200, sort: true, fixed: 'left', totalRowText: '合计：'}
            , {field: 'username', title: 'username', width: 100}
            , {field: 'password', title: 'password', width: 200}
            , {field: 'instId', title: 'instId',width: 100}
            , {field: 'instName', title: 'instName',width: 100}
            , {field: 'enabled', title: 'enabled', width: 100, templet: function(d){ return d.enabled == 1 ? '<span style="color: #5FB878;">ON</span>' : '<span style="color: #FF5722;">OFF</span>'; }}
            , {field: 'upstream', title: 'operation', fixed: 'right', templet: addLink}

//            , {field: 'insertTime', title: 'insert_time', sort: true, width: 300, templet: "<div>{{!d.insertTime?'-':layui.util.toDateString(d.insertTime, 'yyyy-MM-dd HH:mm:ss') }}</div>" }
//            , {field: 'updateTime', title: 'update_time', sort: true, width: 300, templet: "<div>{{!d.updateTime?'-':layui.util.toDateString(d.updateTime, 'yyyy-MM-dd HH:mm:ss') }}</div>" }


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
            localStorage.setItem("pageLimit", this.limit);


            if(res.data && res.data.length == 0){
                if(curr>1){
                    toPage=curr-1;
                    //console.log(toPage);
                    table.reload('demo',{page: {curr:toPage}});
                }
            }
        }
        , response: {
            statusCode: 200
        }
        , parseData: function (res) { //将原始数据解析成 table 组件所规定的数据
            return {
                "code": res.code, //解析接口状态
                "msg": res.description, //解析提示文本
                "count": res.total, //解析数据长度
                "data": res.data //解析数据列表
            };
        }
    });

//头部工具条监听事件
    table.on('toolbar(test)', function (obj) {
        var checkStatus = table.checkStatus(obj.config.id);
        switch (obj.event) {
//            case 'getCheckData':
//                var dataX = checkStatus.data;
//                layer.alert(JSON.stringify(dataX));
//                break;
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
                            url: ctx + '/api/account/del',
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
                layer.open({
                    type: 2,
                    title: 'Add Account',
                    shadeClose: true,
                    shade: 0.8,
                    area: ['500px', '450px'],
                    content: ctx + '/admin/accountEdit',
                    anim: 'slideRight',
                    maxmin: true,
                    skin: 'layui-layer-win10'
                });
                break;
            case 'batchAdd':
                cleanData(false);
                layer.open({
                    type: 1,
                    area: ['800px', '600px'],
                    title: 'Add',
                    content : $('#guideDiv'),
                    anim: 'slideRight',
                    shade: 0.6, // 遮罩透明度
                    shadeClose: true, // 点击遮罩区域，关闭弹层
                    maxmin: true, // 允许全屏最小化
                    skin: 'layui-layer-win10'
                });
                break;
            case 'more':
                var that = this;
                dropdown.render({
                    elem: that,
                    show: true,
                    data: [
                        {title: commonStr.del, id: 'del'},
                        {title: commonStr.enable, id: 'enable'},
                        {title: commonStr.disable, id: 'disable'},
                        // {title: commonStr.download, id: 'download'},
                        // {title: commonStr.reset, id: 'reset'},

                        ],
                    click: function(data, othis){
                        var dataX = table.checkStatus(obj.config.id).data;
                        var url = ctx + '/api/account/'+ data.id;
                        var confirmMsg = commonStr.confirm + ' ' + data.title +'?';
                        var allId = [];
                        if (dataX.length === 0) {
                            layer.msg(commonStr.pleaseSelect);
                        } else {
                            layer.confirm(confirmMsg , function(index) {
                                for (let i = 0; i < dataX.length; i++) {
                                    const val = dataX[i];
                                    allId.push(val.id);
                                }
                                $.ajax({
                                    url: url,
                                    type: 'post',
                                    contentType: 'application/json',
                                    data: JSON.stringify({id: allId}),
                                    success:function (data,statusText) {
                                    if(data.code=='200'){
                                        table.reload('demo',{});
                                        layer.msg(commonStr.success);
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
                    },
                    align: 'right', // 右对齐弹出
                    style: 'box-shadow: 1px 1px 10px rgb(0 0 0 / 12%);' //设置额外样式
                })
                dropdown.reload(that,{});
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

                layer.confirm(commonStr.confirmDel, function(index){

                    $.ajax({
                    url: ctx + '/api/account/del',
                    type: 'post',
                    contentType: 'application/json',
                    data: JSON.stringify({id: [obj.data.id] }),
                    success:function (data,statusText) {
                         if(data.code=='200'){
                             obj.del();
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

        } else if (layEvent === 'edit'){
                layer.open({
                    type: 2,
                    title: 'Edit Account',
                    shadeClose: true,
                    shade: 0.8,
                    area: ['500px', '450px'],
                    content: ctx + '/admin/accountEdit-' + obj.data.id,
                    anim: 'slideRight',
                    maxmin: true,
                    skin: 'layui-layer-win10'
                });
            }else if (layEvent === 'acl'){
                layer.open({
                    type: 2,
                    title: 'Edit ACL - ' + obj.data.username,
                    shadeClose: true,
                    shade: 0.8,
                    area: ['800px', '600px'],
                    content: ctx + '/admin/accountAclEdit-' + obj.data.id,
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
  });})