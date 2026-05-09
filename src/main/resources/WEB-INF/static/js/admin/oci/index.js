layui.use(function(){

var layer = layui.layer;
var laypage = layui.laypage;
var table = layui.table;
var form = layui.form;
var element = layui.element;

// 获取 URL 参数
function getUrlParam(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURIComponent(r[2]);
    return null;
}

var manifestId = getUrlParam('manifestId');
var tagName = getUrlParam('tagName');
var mediaType = getUrlParam('mediaType');

if (!manifestId) {
    layer.msg('缺少 manifestId 参数');
}

// 判断是 manifest list 还是单 manifest
function isManifestList() {
    return mediaType && (mediaType.includes('index') || mediaType.includes('manifest.list'));
}

// 加载 OCI 信息
function loadOciInfo() {
    if (!manifestId) return;

    $.ajax({
        url: ctx + '/api/artifact/queryOciByManifest',
        type: 'post',
        contentType: 'application/json',
        data: JSON.stringify({manifestId: manifestId}),
        success: function(data) {
            if (data.code == '200' && data.data && data.data.length > 0) {
                var oci = data.data[0];
                
                // 判断类型并显示不同内容
                if (isManifestList()) {
                    // Manifest List - 多架构镜像
                    loadManifestListInfo(oci, data.data);
                } else {
                    // Single Manifest - 单架构镜像
                    loadSingleManifestInfo(oci, data.data);
                }
            } else {
                layer.msg('未找到 OCI 信息');
            }
        },
        error: function() {
            layer.alert(commonStr.errorInfo);
        }
    });
}

// 加载 Manifest List 信息（多架构）
function loadManifestListInfo(oci, ociList) {
    // 填充基本信息
    $('#tag_name').text(oci.tagName || '-');
    $('#full_name').text(oci.fullName || '-');
    $('#manifest_list_digest').text(oci.manifestListDigest || '-');
    $('#parent_media_type').text(oci.parentMediaType || '-');
    $('#manifest_list_size').text(oci.manifestListSize || '-');
    $('#manifest_list_created').text(oci.manifestListCreated ? layui.util.toDateString(oci.manifestListCreated, 'yyyy-MM-dd HH:mm:ss') : '-');
    $('#config_digest').text(oci.configDigest || '-');
    $('#arch_count').text(ociList.length);
    
    // 加载子 manifest 列表
    loadChildManifests(ociList);
}

// 加载单 Manifest 信息（单架构）
function loadSingleManifestInfo(oci, ociList) {
    // 填充基本信息
    $('#tag_name').text(oci.tagName || '-');
    $('#full_name').text(oci.fullName || '-');
    $('#manifest_list_digest').text(oci.childDigest || '-');
    $('#parent_media_type').text(oci.childMediaType || '-');
    $('#manifest_list_size').text(oci.childSize || '-');
    $('#manifest_list_created').text(oci.childCreated ? layui.util.toDateString(oci.childCreated, 'yyyy-MM-dd HH:mm:ss') : '-');
    $('#config_digest').text(oci.configDigest || '-');
    $('#arch_count').text('1 (Single Arch)');
    
    // 隐藏子 manifest 表格，显示单架构信息
    $('#arch_count').parent().html('<p><strong>Type:</strong> Single Architecture</p>' +
        '<p><strong>Platform:</strong> ' + (oci.os || 'unknown') + '/' + (oci.osArch || 'unknown') + (oci.variant ? '/' + oci.variant : '') + '</p>');
    
    // 不加载子 manifest 列表
    table.render({
        elem: '#demo',
        data: ociList,
        page: false,
        cols: [[
            {type: 'numbers', title: '序号', width: 60}
            , {field: 'info', title: 'Information', templet: function(d){
                return '<div style="padding: 20px; text-align: center; color: #999;">' +
                    'This is a single-architecture manifest.<br/>' +
                    'Platform: ' + (d.os || 'unknown') + '/' + (d.osArch || 'unknown') + (d.variant ? '/' + d.variant : '') +
                    '</div>';
            }}
        ]]
    });
}

// 加载子 manifest 列表
function loadChildManifests(ociList) {
    table.render({
        elem: '#demo'
        , height: 'full-30'
        , even: true
        , data: ociList
        , title: 'OCI 架构列表'
        , page: false
        , toolbar: '#toolbarDemo'
        , defaultToolbar: ['filter', 'exports', 'print', {
            title: '提示'
            , layEvent: 'LAYTABLE_TIPS'
            , icon: 'layui-icon-tips'
        }]
        , totalRow: false
        , cols: [[
            {type: 'numbers', title: '序号', width: 60}
            , {field: 'platform', title: 'Platform', width: 200, templet: function(d){
                // 合并显示：os/arch[/variant]，类似 docker hub 格式
                var platform = d.os || 'unknown';
                if (d.osArch) {
                    platform += '/' + d.osArch;
                }
                if (d.variant) {
                    platform += '/' + d.variant;
                }
                return '<span style="color: green; font-weight: bold;">' + platform + '</span>';
            }}
            , {field: 'childDigest', title: 'Digest', width: 500, templet: function(d){
                return '<span style="font-family: monospace; font-size: 11px;">' + (d.childDigest || '-') + '</span>';
            }}
            , {field: 'childSize', title: 'Size', width: 120, templet: function(d){
                return formatSize(d.childSize);
            }}
            , {field: 'childMediaType', title: 'Media Type', width: 280, templet: function(d){
                return '<span style="font-size: 10px;">' + (d.childMediaType || '-') + '</span>';
            }}
            , {field: 'childCreated', title: 'Created', width: 160, templet: function(d){
                return d.childCreated ? layui.util.toDateString(d.childCreated, 'yyyy-MM-dd HH:mm:ss') : '-';
            }}
            , {field: 'upstream', title: 'Operation', fixed: 'right', templet: function(d){
                let copyBtn = '<button type="button" class="layui-btn layui-btn-normal layui-btn-xs" lay-event="copy">Copy</button>'
                let pullBtn = '<button type="button" class="layui-btn layui-btn-warm layui-btn-xs" lay-event="pull">Pull</button>'
                let detailBtn = '<button type="button" class="layui-btn layui-btn-primary layui-btn-xs" lay-event="detail">Annotations</button>'
                return [copyBtn, pullBtn, detailBtn].join("&nbsp;");
            }}
        ]]
        , done: function(res, curr, count){
            localStorage.setItem('pageLimit', this.limit);
        }
        , response: {
            statusCode: 200
        }
    });
}

// 格式化大小
function formatSize(bytes) {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + ' MB';
    return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB';
}

// 复制 Digest
function copyDigest(digest) {
    var input = document.createElement('input');
    input.value = digest;
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
    layer.msg('已复制到剪贴板');
}

// 显示 Pull 命令
function showPullCommand(digest, tagName) {
    var repoName = tagName ? tagName.split(':')[0] : 'repository';
    var pullCmd = 'docker pull 192.168.66.125:5000/' + repoName + '@' + digest;
    
    layer.open({
        type: 1,
        area: ['600px', '200px'],
        title: 'Pull Command',
        content: '<div style="padding: 20px;">' +
                 '<p style="margin-bottom: 10px;"><strong>Command:</strong></p>' +
                 '<textarea style="width: 100%; height: 80px; font-family: monospace;" readonly>' + pullCmd + '</textarea>' +
                 '<button type="button" class="layui-btn layui-btn-normal layui-btn-sm" style="margin-top: 10px;" onclick="navigator.clipboard.writeText(\'' + pullCmd + '\');layer.msg(\'已复制\');">复制命令</button>' +
                 '</div>',
        shade: 0.6,
        shadeClose: true
    });
}

// 显示 Annotations
function showAnnotations(annotations) {
    if (!annotations) {
        layer.msg('No annotations');
        return;
    }
    
    try {
        var annObj = JSON.parse(annotations);
        var content = '<div style="padding: 10px; max-height: 400px; overflow-y: auto;">';
        layui.each(annObj, function(key, value) {
            content += '<p style="margin: 5px 0;"><strong style="color: #1E9FFF;">' + key + ':</strong> ' + value + '</p>';
        });
        content += '</div>';
        
        layer.open({
            type: 1,
            area: ['600px', '500px'],
            title: 'Annotations',
            content: content,
            shade: 0.6,
            shadeClose: true
        });
    } catch (e) {
        layer.msg('Invalid annotations JSON');
    }
}

// 监听工具条事件
table.on('tool(test)', function(obj){
    var data = obj.data;
    var layEvent = obj.event;
    
    if (layEvent === 'copy') {
        copyDigest(data.childDigest);
    } else if (layEvent === 'pull') {
        showPullCommand(data.childDigest, data.tagName);
    } else if (layEvent === 'detail') {
        showAnnotations(data.annotations);
    }
});

// 复制 Manifest List Digest
$('#copyDigest').click(function(){
    var digest = $('#manifest_list_digest').text();
    if (digest && digest !== '-') {
        copyDigest(digest);
    }
});

// 查看 Pull 命令
$('#pullCommand').click(function(){
    var digest = $('#manifest_list_digest').text();
    var tagName = $('#tag_name').text();
    if (digest && digest !== '-') {
        showPullCommand(digest, tagName);
    }
});

// 页面加载时初始化
$(document).ready(function(){
    loadOciInfo();
});

});
