var layer;
var element;
var form;
var laypage;
var laydate;

// 使用layui内部jQuery
var $ = layui.$;
var jQuery = layui.$;

layui.$.ajaxSetup({
    complete: function(xhr,status) {
//        console.log(xhr.status);
//        console.log(ctx);
//        console.log(xhr.statusText);
//        console.log(xhr.responseText);
        if (xhr.status == 401) {
            window.location.href = ctx;
        }
    }
});


$(function() {

    layer = layui.layer;
    element = layui.element;
    form = layui.form;
    laypage = layui.laypage;
    dropdown = layui.dropdown;

    // 日期控件
    layui.use('laydate', function() {
        laydate = layui.laydate;

        // 执行laydate实例
        $(".laydate").each(function() {
            $(this).attr("id", "date_" + guid());
            $(this).attr("readonly", true);

            laydate.render({
                elem: "#" + $(this).attr("id"), // 指定元素
                type: 'date',
                trigger: 'click',
                format: 'yyyy-MM-dd' // 可任意组合
            });
        })
    });

    form.render();

    // 关闭input自动填充
    $("input").attr("autocomplete", "off");

    // 菜单选中
    var url = location.pathname + location.search;
    $("a[href='" + ctx + url + "']").parent().addClass("layui-this");

    //初始化aceMode默认值
    if ($('#addForm1 select[name="aceMode"]').length) {
        var aceMode = localStorage.getItem(lastPath+'_aceMode');
        if ('' == aceMode || null == aceMode || undefined == aceMode) {
            aceMode = cfg.aceMode;
        }
        $('#addForm1 select[name="aceMode"]').val(aceMode);
        layui.form.render('select');
    }
    //aceMode切换事件
    form.on('select(aceMode)', function(data){
        var elem = data.elem; // 获得 select 原始 DOM 对象
        var value = data.value; // 获得被选中的值
        var othis = data.othis; // 获得 select 元素被替换后的 jQuery 对象
        var text = editor.getValue();
        var modeName = editor.session.getMode().$id;
        if (/yaml/.test(modeName)){
            try{
                const jsonData = jsyaml.load(text);
                const jsonText = JSON.stringify(jsonData, null, 2);
                editor.setValue(jsonText,-1);
            } catch(error) {

            }
        }else if (/json/.test(modeName)){
            try{
                const jsonObject = JSON.parse(text);
                const yamlText = jsyaml.dump(jsonObject);
                editor.setValue(yamlText,-1);
            }  catch(error) {

            }
        }

        var path = window.location.pathname;
        var pathArray = path.split('/');
        var lastPath = pathArray[pathArray.length - 1];
        //console.log(lastPath);
        //console.log(value);
        editor.session.setMode("ace/mode/"+value);
        //存储当前页面的aceMode到本地存储
        localStorage.setItem(lastPath+'_aceMode', value);
    });

    // 判断屏幕分辨率, 给table加上lay-size="sm"
    //if (document.body.clientWidth <= 1600) {
        //$(".layui-table").attr("lay-size", "sm");
        //$(".layui-btn").addClass("layui-btn-sm");
    //}

    if ($('#ID-dropdown-demo-base-text').length){
        dropdown.render({
            elem: '#ID-dropdown-demo-base-text'
            ,click: function(obj){
                Cookies.set(COOK_NAME.instId, obj.id, {path: '/', SameSite: "Strict" })
                location.reload();
            },
            templet: function(d){
                var instId=Cookies.get(COOK_NAME.instId);
                if (undefined!=instId && d.id==instId) {
                    return d.title + '&nbsp;&nbsp;<span class="layui-badge-dot layui-bg-green"></span>';
                } else {
                    return d.title;
                }
            }
       });

        $("#ID-dropdown-demo-base-text").click(function(){
            $.ajax({
            type : 'POST',
            url : ctx + '/api/inst/dropdown',
            dataType : 'json',
            success : function(data) {
                    if (data.code == 200) {
                        dropdown.reloadData('ID-dropdown-demo-base-text', {
                            data: data.data
                        })
                    }
                }
            });
        });
    }

    // 导航点击事件
    element.on('nav(test)', function(elem){
        //console.log(elem); // 得到当前点击的元素 jQuery 对象
        //console.log(elem.text().trim());
        var menuKey = elem.attr("id");
        //console.log($(elem).parent().hasClass('layui-nav-itemed'));
        if ($(elem).parent().is('li')){
//            localStorage.setItem('menuToggle_'+menuKey, $(elem).parent().hasClass('layui-nav-itemed') );
//            localStorage.setItem('menuHtml_'+menuKey, $(elem).parent().prop("outerHTML") );
              Cookies.set('menuToggle_'+menuKey, $(elem).parent().hasClass('layui-nav-itemed'), { expires: 365, path: '/', SameSite: "Strict" })
        }
        //layer.msg(elem.text());
    });


})

// 关闭AJAX相应的缓存
$.ajaxSetup({
    cache: false
});


function gohref(url) {
    location.href = url;

}
function i18n(lang){
//    console.log(lang);
//    var cname="SOLON.LOCALE"
////    var now = new Date();
////    now.setTime(now.getTime() - 24 * 60 * 60 * 1000);
////    let expires = "expires="+ now.toUTCString();
//    document.cookie = cname + "=" + lang + ";path=/";
    Cookies.set('SOLON.LOCALE', lang, { expires: 365, path: '/', SameSite: "Strict" })
    location.reload();

//     $.ajax({
//            type : 'POST',
//            url : '/token/i18n',
//            data : {l:lang},
//            dataType : 'json',
//            success : function(data) {
//                //location.reload();
//            }
//        });
}

// 退出登录
function loginOut() {

//    var cname="TOKEN"
//    var cvalue=''
//    //console.log(cvalue)
//    var now = new Date();
//    now.setTime(now.getTime() - 24 * 60 * 60 * 1000);
//    let expires = "expires="+ now.toUTCString();
//    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";

    Cookies.remove(COOK_NAME.token, { path: '/' }) // removed!
    location.href = ctx + "/admin/login";

}



// 日期格式化
Date.prototype.format = function(format) {
    var date = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "H+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3),
        "S+": this.getMilliseconds()
    };
    if (/(y+)/i.test(format)) {
        format = format.replace(RegExp.$1, (this.getFullYear() + '')
            .substr(4 - RegExp.$1.length));
    }
    for (var k in date) {
        if (new RegExp("(" + k + ")").test(format)) {
            format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? date[k]
                : ("00" + date[k]).substr(("" + date[k]).length));
        }
    }
    return format;
}

function formatDate(now) {
    if (now == null || now == '') {
        return "";
    }

    return new Date(now).format("yyyy-MM-dd HH:mm:ss");
}

// 查看图片
function seePic(url) {
    window.open(url);
}

// 生成uuid
function S4() {
    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
}
function guid() {
    return (S4() + S4() + "-" + S4() + "-" + S4() + "-" + S4() + "-" + S4() + S4() + S4());
}

// 时间字符串转时间戳
function strToTime(str) {
    var str = str.replace(/-/g, '/');
    var timestamp = new Date(str).getTime();

    return timestamp
}

// 获取url参数
function getQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null)
        return unescape(r[2]);
    return null;
}

// 下载文件
function downloadFile(url, name) {
    window.open(ctx + "/downloadFile?url=" + encodeURIComponent(url) + "&name=" + encodeURIComponent(name));
}



// form转json
function form2JsonString(formId) {
    let fm = formId.startsWith('#') ? formId : ('#' + formId);
    var paramArray = $(fm).serializeArray();
    /* 请求参数转json对象 */
    var jsonObj = {};
    $(paramArray).each(function() {
        jsonObj[this.name] = this.value;
    });
    return JSON.stringify(jsonObj);

}

var loaded;
function autoUpdate(url) {
    if (confirm(commonStr.confirmUpdate)) {
        loaded = layer.load();
        $.ajax({
            type: 'POST',
            url: ctx + '/api/autoUpdate',
            data: {
                url: url
            },
            dataType: 'json',
            success: function(data) {
                if (!data.success) {
                    layer.close(loaded);
                    layer.alert(data.msg);
                    return;
                }

                setTimeout(function() {
                    layer.close(loaded);
                    layer.alert(commonStr.updateOver);
                }, 10000)


            },
            error: function() {
                setTimeout(function() {
                    layer.close(loaded);
                    layer.alert(commonStr.updateOver);
                }, 10000)
            }
        });
    }

}





function setParamOrder(id, seq) {
    if (seq == -1) {
        // 前移
        var prev = $("#" + id).prev();
        $("#" + id).after(prev);
    } else {
        // 后移
        var next = $("#" + id).next();
        $("#" + id).before(next);
    }
}

// 显示载入框
var loadIndex;
function showLoad() {
    loadIndex = layer.load();
}
function closeLoad() {
    layer.close(loadIndex);
}

// 显示使用流程
function showHelp() {
    console.log("showHelp");
}

