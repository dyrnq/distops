
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

    var default_limit = localStorage.getItem('pageLimit');
    if ('' == default_limit || null == default_limit || undefined == default_limit) {
        default_limit = cfg.pageLimit;
    }

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
       layui.form.render('checkbox');
    });

    cleanData(false);
    $.ajax({
        url: ctx + '/api/inst/get',
        type: 'post',
        contentType: 'application/json',
        data: JSON.stringify({id: id}),
        success: function (data, statusText) {
            if (data.code == '200') {
                $('#addForm1 input, #addForm1 select, #addForm1 textarea, #addForm1 checkbox').each(function () {
                    var elementName = $(this).attr('name');
                    if (elementName in data.data) { // 判断对象中是否有该属性
                        $(this).val(data.data[elementName]); // 将属性对应的值填充到表单元素中
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

            } else {
                layer.msg(data.description);
            }
        },
        'error': function () {
            layer.msg(commonStr.errorInfo);
        }
    });

});