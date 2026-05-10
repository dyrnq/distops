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
    layer.msg('missing manifestId');
}

// 判断是 manifest list 还是单 manifest
function isManifestList() {
    return mediaType && (mediaType.includes('index') || mediaType.includes('manifest.list'));
}

// 加载 Manifest 信息
function loadOciInfo() {
    if (!manifestId) return;

    $.ajax({
        url: ctx + '/api/artifact/queryManifest',
        type: 'post',
        contentType: 'application/json',
        data: JSON.stringify({manifestId: manifestId}),
        success: function(data) {
            if (data.code == '200' && data.data) {
                if (Array.isArray(data.data) && data.data.length > 0) {
                    // OCI manifest list (multi-arch)
                    var oci = data.data[0];
                    loadManifestListInfo(oci, data.data);
                } else if (data.data.digest) {
                    // Single manifest returned as object
                    loadSingleManifestDetail(data.data);
                } else {
                    layer.msg('Manifest not found');
                }
            } else {
                layer.msg('Manifest not found');
            }
        },
        error: function() {
            layer.alert(commonStr.errorInfo);
        }
    });
}

// 加载 Manifest List 信息（多架构）
function loadManifestListInfo(oci, ociList) {
    // Set OCI-specific labels
    $('#pageTitle').text('OCI Image Index');
    $('#cardTitle').text('OCI Manifest List');
    $('#digestLabel').text('Manifest List Digest');
    $('#archInfo').html('<strong>Architecture Count:</strong> <span id="arch_count"></span>');

    // 填充基本信息
    $('#tag_name').text(oci.tagName || '-');
    $('#full_name').text(oci.fullName || '-');
    $('#manifest_list_digest').text(oci.manifestListDigest || '-');
    $('#parent_media_type').text(oci.parentMediaType || '-');
    $('#manifest_list_size').text(formatSize(oci.manifestListSize));
    $('#manifest_list_created').text(oci.manifestListCreated ? layui.util.toDateString(oci.manifestListCreated, 'yyyy-MM-dd HH:mm:ss') : '-');
    $('#config_digest').text(oci.configDigest || '-');
    $('#arch_count').text(ociList.length);
    window._currentRepoName = oci.repoName || '';

    // 加载子 manifest 列表
    loadChildManifests(ociList);
}

// 加载单 Manifest 详情（从 API 返回的 Manifest 对象）
function loadSingleManifestDetail(manifest) {
    $('#pageTitle').text('Image Manifest');
    $('#cardTitle').text('Manifest Detail');
    $('#digestLabel').text('Manifest Digest');
    $('#archInfo').html('<p><strong>Type:</strong> <span style="color: #FFB800;">Single Architecture</span></p><p><strong>Platform:</strong> <span id="platformInfo"></span></p>');

    $('#tag_name').text(tagName || '-');
    $('#full_name').text(tagName || '-');
    $('#manifest_list_digest').text(manifest.digest || '-');
    $('#parent_media_type').text(manifest.mediaType || '-');
    $('#manifest_list_size').text(formatSize(manifest.size));
    window._currentRepoName = (tagName ? tagName.split(':')[0] : 'repo');
    $('#manifest_list_created').text(manifest.created ? layui.util.toDateString(manifest.created, 'yyyy-MM-dd HH:mm:ss') : '-');
    $('#config_digest').text(manifest.configDigest || '-');

    var platform = (manifest.os || 'unknown') + '/' + (manifest.osArch || 'unknown') + (manifest.variant ? '/' + manifest.variant : '');
    $('#platformInfo').text(platform).css({color: 'green', fontWeight: 'bold'});

    var repoName = tagName ? tagName.split(':')[0] : 'repo';
    var commands = buildPullCommands(manifest.digest, repoName);
    window._pullCommands = commands;

    var detailHtml = '<div style="padding: 20px;">';
    detailHtml += '<table class="layui-table"><colgroup><col width="150"><col></colgroup><tbody>';
    detailHtml += '<tr><td><strong>Type</strong></td><td><span style="color: #FFB800;">Single Architecture</span></td></tr>';
    detailHtml += '<tr><td><strong>Platform</strong></td><td><span style="color: green; font-weight: bold;">' + platform + '</span></td></tr>';
    detailHtml += '<tr><td><strong>Digest</strong></td><td style="font-family: monospace; font-size: 12px;">' + (manifest.digest || '-') + '</td></tr>';
    detailHtml += '<tr><td><strong>Media Type</strong></td><td style="font-size: 12px;">' + (manifest.mediaType || '-') + '</td></tr>';
    detailHtml += '<tr><td><strong>Size</strong></td><td>' + formatSize(manifest.size) + '</td></tr>';
    detailHtml += '<tr><td><strong>Created</strong></td><td>' + (manifest.created ? layui.util.toDateString(manifest.created, 'yyyy-MM-dd HH:mm:ss') : '-') + '</td></tr>';
    detailHtml += '<tr><td><strong>Config Digest</strong></td><td style="font-family: monospace; font-size: 12px;">' + (manifest.configDigest || '-') + '</td></tr>';

    for (var i = 0; i < commands.length; i++) {
        var label = (i === 0) ? '<strong>Pull Commands</strong>' : '';
        detailHtml += '<tr><td>' + label + '</td><td style="padding: 4px;">';
        detailHtml += '<span style="font-weight: bold; color: #1E9FFF;">' + commands[i].name + ':</span> ';
        detailHtml += '<code style="background: #f0f0f0; padding: 4px 8px; border-radius: 4px; font-size: 12px; word-break: break-all; display: block; margin: 2px 0;">' + commands[i].cmd + '</code>';
        detailHtml += copyBtnHtml(i);
        detailHtml += '</td></tr>';
    }
    detailHtml += '</tbody></table>';

    if (manifest.annotations) {
        try {
            var annObj = JSON.parse(manifest.annotations);
            detailHtml += '<fieldset class="layui-elem-field" style="margin-top: 15px;"><legend>Annotations</legend><div class="layui-field-box">';
            layui.each(annObj, function(key, value) {
                detailHtml += '<p style="margin: 4px 0;"><strong style="color: #1E9FFF;">' + key + ':</strong> ' + value + '</p>';
            });
            detailHtml += '</div></fieldset>';
        } catch(e) {}
    }
    detailHtml += '</div>';

    $('#demo').html(detailHtml);
}

// Copy pull command for single arch


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
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(digest).then(function() {
            layer.msg('已复制到剪贴板');
        });
    } else {
        var ta = document.createElement('textarea');
        ta.value = digest;
        ta.style.position = 'fixed';
        ta.style.left = '-9999px';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        layer.msg('已复制到剪贴板');
    }
}

// 生成 pull 命令列表
function buildPullCommands(digest, repoName) {
    var base = '192.168.66.125:5000/' + (repoName || 'repo') + '@' + digest;
    return [
        { name: 'docker', cmd: 'docker pull ' + base },
        { name: 'nerdctl', cmd: 'nerdctl pull ' + base },
        { name: 'ctr', cmd: 'ctr image pull ' + base }
    ];
}

// 生成 copy 按钮 HTML
function copyBtnHtml(index) {
    return '<button type="button" class="layui-btn layui-btn-xs layui-btn-normal" onclick="copyPullCmdByIndex(' + index + ')">Copy</button>';
}

// 显示 Pull 命令
function showPullCommand(digest, repoName) {
    var commands = buildPullCommands(digest, repoName);
    window._pullCommands = commands;

    var html = '<div style="padding: 20px;">';
    for (var i = 0; i < commands.length; i++) {
        html += '<div style="margin-bottom: 12px;">';
        html += '<strong>' + commands[i].name + ':</strong>';
        html += '<div style="display: flex; align-items: center; gap: 8px; margin-top: 4px;">';
        html += '<code style="flex: 1; background: #f0f0f0; padding: 6px 10px; border-radius: 4px; font-size: 13px; word-break: break-all;">' + commands[i].cmd + '</code>';
        html += copyBtnHtml(i);
        html += '</div></div>';
    }
    html += '</div>';

    layer.open({
        type: 1,
        area: ['70%', '50%'],
        title: 'Pull Commands',
        content: html,
        shade: 0.6,
        shadeClose: true
    });
}

// 通过 index 复制 pull 命令




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
        showPullCommand(data.childDigest, data.repoName);
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
    var repoName = window._currentRepoName || 'repo';
    if (digest && digest !== '-') {
        showPullCommand(digest, repoName);
    }
});

// Global function for popup copy button (called from inline onclick)


// 页面加载时初始化
$(document).ready(function(){
    loadOciInfo();
});

});

// Global function for copy buttons (called from inline onclick)
window.copyPullCmdByIndex = function(index) {
    var cmd = '';
    if (window._pullCommands && window._pullCommands[index]) {
        cmd = window._pullCommands[index].cmd;
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(cmd).then(function() {
            layer.msg('已复制');
        });
    } else {
        var ta = document.createElement('textarea');
        ta.value = cmd;
        ta.style.position = 'fixed';
        ta.style.left = '-9999px';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        layer.msg('已复制');
    }
};
