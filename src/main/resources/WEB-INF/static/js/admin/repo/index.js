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
    $('#addForm1 input[name="isManifestList"]').prop('checked', true);
}

function addLink(d) {
    let editBtn = '<button type="button" class="layui-btn layui-btn-normal layui-btn-xs" lay-event="edit">' + commonStr.edit + '</button>'
    let delBtn  = '<button type="button" class="layui-btn layui-btn-danger layui-btn-xs" lay-event="del">' + commonStr.del + '</button>'
    let artifactBtn  = '<button type="button" class="layui-btn layui-btn-normal layui-btn-xs" lay-event="artifacts">Artifacts</button>'
    return [artifactBtn, editBtn, delBtn].join("&nbsp;");
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

form.on('switch(demo-checkbox-filter)', function(data){
    var elem = data.elem;
    var checked = elem.checked;
    var value = elem.value;
    if(checked){
        $(elem).val("1");
    }else{
        $(elem).val("0");
    }
    layui.form.render('checkbox');
});

form.on('submit(demo-table-search)', function(data){
     var field = data.field;
     table.reload('demo', {
       page: {
         curr: 1
       },
       where: field
     });
     return false;
});

$('#add').click(function(){
    cleanData(false);
    layer.open({
        type: 1,
        area: ['800px', '600px'],
        title: 'Add',
        content : $('#windowDiv'),
        anim: 'slideRight',
        shade: 0.6,
        shadeClose: true,
        maxmin: true,
        skin: 'layui-layer-win10'
    });
});

$('#addOver').click(function(){
    let u = $('#addForm1 input[name="u"]').val();
    var formData = $('#addForm1').serializeArray();
    var fieldName = 'isManifestList';
    $.each(formData, function() {
      if (this.name === fieldName) {
        if ( $('#addForm1 input[name="isManifestList"]').prop('checked') === false){
            this.value = '0';
        }else{
            this.value = '1';
        }
      }
    });
    var serializedData = formData.reduce(function(acc, curr) {
      return acc + (acc ? '&' : '') + curr.name + '=' + curr.value;
    }, '');

    $.ajax({
        type : 'POST',
        url: ctx + '/api/repo/'+u,
        data : serializedData,
        dataType : 'json',
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

    table.render({
        elem: '#demo'
        , height: 'full-30'
        , even: true
        , url: ctx + '/api/repo'
        , title: 'repo 表'
        , page: true
        , limit: default_limit
        , limits: cfg.pageLimits
        , toolbar: '#toolbarDemo'
        , defaultToolbar: ['filter', 'exports', 'print', {
            title: '提示'
            , layEvent: 'LAYTABLE_TIPS'
            , icon: 'layui-icon-tips'
        }]
        , totalRow: false
        , cols: [[
            {type: 'checkbox', fixed: 'left'}
            , {field: 'id', title: 'id', width: 200, sort: true, fixed: 'left'}
            , {field: 'instId', title: 'instId', width: 150}
            , {field: 'instName', title: 'instName', width: 150}
            , {field: 'repoName', title: 'repo_name', width: 300}
            , {field: 'artifactCount', title: 'artifact_count', width: 200, sort: true}
            , {field: 'upstream', title: 'operation', fixed: 'right', templet: addLink}
        ]]
        , done: function (res, curr, count){
            var thisOptions = table.getOptions('demo');
            localStorage.setItem('pageLimit', thisOptions.limit);
            if(res.data && res.data.length == 0){
                if(curr>1){
                    toPage=curr-1;
                    table.reload('demo',{page: {curr:toPage}});
                }
            }
        }
        , response: {
            statusCode: 200
        }
        , parseData: function (res) {
            return {
                "code": res.code,
                "msg": res.description,
                "count": res.total,
                "data": res.data
            };
        }
    });

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
                            allId.push(val.id)
                        }
                        $.ajax({
                            url: ctx + '/api/repo/del',
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
                    shade: 0.6,
                    shadeClose: true,
                    maxmin: true,
                    skin: 'layui-layer-win10'
                });
                break;
            case 'more':
                var that = this;
                dropdown.render({
                    elem: that,
                    show: true,
                    data: [
                        {title: commonStr.del, id: 'del'}
                    ],
                    click: function(data, othis){
                        var dataX = table.checkStatus(obj.config.id).data;
                        var url = ctx + '/api/repo/'+ data.id;
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
                    align: 'right',
                    style: 'box-shadow: 1px 1px 10px rgb(0 0 0 / 12%);'
                })
                dropdown.reload(that,{});
              break;
        }
    });

    table.on('tool(test)', function(obj){
        var data = obj.data;
        var layEvent = obj.event;

        if(layEvent === 'del'){
                layer.confirm(commonStr.confirmDel, function(index){
                    $.ajax({
                    url: ctx + '/api/repo/del',
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
                cleanData(true);
                $.ajax({
                    url: ctx + '/api/repo/get',
                    type:'post',
                    contentType: 'application/json',
                    data:JSON.stringify({id:obj.data.id}),
                    success:function (data,statusText) {
                        if(data.code=='200'){
                            $('#addForm1 input, #addForm1 select, #addForm1 textarea, #addForm1 checkbox').each(function() {
                              var elementName = $(this).attr('name');
                              if (elementName in data.data) {
                                $(this).val(data.data[elementName]);
                              }
                              if ( elementName == 'isManifestList') {
                                if (data.data[elementName] == 1) {
                                    $(this).prop('checked', true);
                                }else {
                                    $(this).prop('checked', false);
                                }
                              }
                            });
                            layui.form.render('select');
                            layui.form.render('checkbox');

                            layer.open({
                                type: 1,
                                area: ['800px', '600px'],
                                title: 'Edit',
                                content : $('#windowDiv'),
                                anim: 'slideRight',
                                shade: 0.6,
                                shadeClose: true,
                                maxmin: true,
                                skin: 'layui-layer-win10'
                            });
                        }else{
                             layer.msg(data.description);
                        }
                    },
                    'error':function () {
                        layer.msg(commonStr.errorInfo);
                    }
                });
        } else if (layEvent === 'artifacts'){
            // 打开 artifact 列表页面，传递 repo_id 参数
            window.open(ctx + '/admin/artifact?repoId=' + obj.data.id, '_blank');
        }
    });
});
