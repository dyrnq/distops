layui.use(function(){
  var element = layui.element;
  var table = layui.table;


// 已知数据渲染
  var inst = table.render({
    elem: '#ID-table-demo-data',
    cols: [[ //标题栏
      {field: 'id', title: 'git',templet: '<div><a class="layui-table-link" href="https://gitee.com/{{= d.id }}" target="_blank">@{{= d.id }}</a></div>'},
      {field: 'qq', title: 'qq'},
      {field: 'email', title: 'email'}
    ]],
    data: [
    {
      "id": "moonhunters",
      "qq": "1343093151",
      "email": "1343093151@qq.com"
    }
    ,{
      "id": "dyrnq",
      "qq": "2627529678",
      "email": "dyrnq@qq.com"
    }
    ],
//    skin: 'line', // 表格风格
//    even: true,
    page: false
  });


  // hash 地址定位
  var hashName = 'tabid'; // hash 名称
  var layid = location.hash.replace(new RegExp('^#'+ hashName + '='), ''); // 获取 lay-id 值

  // 初始切换
  element.tabChange('test-hash', layid);
  // 切换事件
  element.on('tab(test-hash)', function(obj){
    location.hash = hashName +'='+ this.getAttribute('lay-id');
  });
});