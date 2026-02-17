<script setup lang="ts">
import { ref } from 'vue'
import { message, notification, Modal } from 'ant-design-vue'

/**
 * Ant Design Vue 组件预览页面
 * @description 展示所有常用的 Ant Design Vue 组件
 */

// 表单相关数据
const inputValue = ref('')
const textareaValue = ref('')
const selectValue = ref('option1')
const cascaderValue = ref([])
const dateValue = ref()
const timeValue = ref()
const switchValue = ref(true)
const sliderValue = ref(30)
const rateValue = ref(3.5)
const checkboxValue = ref(['apple'])
const radioValue = ref('a')
const transferTargetKeys = ref(['1', '3'])
const uploadFileList = ref([])
const treeCheckedKeys = ref(['0-0-0'])
const currentTab = ref('1')
const collapseActiveKey = ref(['1'])
const current = ref(0)
const spinning = ref(false)

// 表格数据
const columns = [
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '年龄', dataIndex: 'age', key: 'age' },
  { title: '地址', dataIndex: 'address', key: 'address' },
  { title: '操作', key: 'action', fixed: 'right', width: 100 }
]

const dataSource = [
  { key: '1', name: '张三', age: 32, address: '北京市朝阳区' },
  { key: '2', name: '李四', age: 42, address: '上海市浦东新区' },
  { key: '3', name: '王五', age: 32, address: '深圳市南山区' }
]

// Select 选项
const selectOptions = [
  { value: 'option1', label: '选项1' },
  { value: 'option2', label: '选项2' },
  { value: 'option3', label: '选项3' }
]

// Cascader 选项
const cascaderOptions = [
  {
    value: 'zhejiang',
    label: '浙江',
    children: [
      { value: 'hangzhou', label: '杭州' },
      { value: 'ningbo', label: '宁波' }
    ]
  },
  {
    value: 'jiangsu',
    label: '江苏',
    children: [
      { value: 'nanjing', label: '南京' },
      { value: 'suzhou', label: '苏州' }
    ]
  }
]

// Tree 数据
const treeData = [
  {
    title: '父节点1',
    key: '0-0',
    children: [
      { title: '子节点1-1', key: '0-0-0' },
      { title: '子节点1-2', key: '0-0-1' }
    ]
  },
  { title: '父节点2', key: '0-1' }
]

// Transfer 数据
const transferData = Array.from({ length: 20 }).map((_, i) => ({
  key: i.toString(),
  title: `选项 ${i + 1}`,
  description: `描述 ${i + 1}`
}))

// 菜单数据
const selectedMenuKeys = ref(['1'])
const menuItems = [
  { key: '1', icon: 'MailOutlined', label: '导航一', title: '导航一' },
  { key: '2', icon: 'AppstoreOutlined', label: '导航二', title: '导航二' },
  { key: '3', icon: 'SettingOutlined', label: '导航三', title: '导航三' }
]

// Steps 步骤
const steps = [
  { title: '第一步', description: '这是第一步的描述' },
  { title: '第二步', description: '这是第二步的描述' },
  { title: '第三步', description: '这是第三步的描述' }
]

// 事件处理函数
const showMessage = () => {
  message.success('这是一条成功提示')
}

const showNotification = () => {
  notification.open({
    message: '通知标题',
    description: '这是通知的详细内容，可以包含更多信息。'
  })
}

const showModal = () => {
  Modal.info({
    title: '对话框标题',
    content: '这是对话框的内容'
  })
}

const handleTransferChange = (keys: string[]) => {
  transferTargetKeys.value = keys
}

const nextStep = () => {
  if (current.value < steps.length - 1) {
    current.value++
  }
}

const prevStep = () => {
  if (current.value > 0) {
    current.value--
  }
}

const toggleSpin = () => {
  spinning.value = !spinning.value
  if (spinning.value) {
    setTimeout(() => {
      spinning.value = false
    }, 3000)
  }
}
</script>

<template>
  <div class="antd-preview">
    <a-layout>
      <a-layout-header class="header">
        <h1>Ant Design Vue 组件预览</h1>
      </a-layout-header>
      <a-layout-content class="content">
        <!-- 通用组件 -->
        <a-card title="Button 按钮" class="section-card">
          <a-space wrap>
            <a-button type="primary">Primary</a-button>
            <a-button>Default</a-button>
            <a-button type="dashed">Dashed</a-button>
            <a-button type="text">Text</a-button>
            <a-button type="link">Link</a-button>
            <a-button danger>Danger</a-button>
            <a-button disabled>Disabled</a-button>
            <a-button type="primary" loading>Loading</a-button>
            <a-button type="primary" size="large">Large</a-button>
            <a-button type="primary" size="small">Small</a-button>
          </a-space>
        </a-card>

        <a-card title="Typography 排版" class="section-card">
          <a-space direction="vertical" style="width: 100%">
            <a-typography-title>h1. Ant Design</a-typography-title>
            <a-typography-title :level="2">h2. Ant Design</a-typography-title>
            <a-typography-title :level="3">h3. Ant Design</a-typography-title>
            <a-typography-paragraph>
              这是一段普通文本。Ant Design 是一套企业级 UI 设计语言和 React 组件库。
            </a-typography-paragraph>
            <a-typography-text type="secondary">次要文本</a-typography-text>
            <a-typography-text type="success">成功文本</a-typography-text>
            <a-typography-text type="warning">警告文本</a-typography-text>
            <a-typography-text type="danger">危险文本</a-typography-text>
            <a-typography-text strong>加粗文本</a-typography-text>
            <a-typography-text italic>斜体文本</a-typography-text>
            <a-typography-text underline>下划线文本</a-typography-text>
            <a-typography-text delete>删除线文本</a-typography-text>
          </a-space>
        </a-card>

        <!-- 布局组件 -->
        <a-card title="Layout 布局" class="section-card">
          <a-row :gutter="16">
            <a-col :span="6">
              <div class="demo-col">col-6</div>
            </a-col>
            <a-col :span="6">
              <div class="demo-col">col-6</div>
            </a-col>
            <a-col :span="6">
              <div class="demo-col">col-6</div>
            </a-col>
            <a-col :span="6">
              <div class="demo-col">col-6</div>
            </a-col>
          </a-row>
          <a-divider />
          <a-row :gutter="16">
            <a-col :span="8">
              <div class="demo-col">col-8</div>
            </a-col>
            <a-col :span="8">
              <div class="demo-col">col-8</div>
            </a-col>
            <a-col :span="8">
              <div class="demo-col">col-8</div>
            </a-col>
          </a-row>
        </a-card>

        <a-card title="Space 间距" class="section-card">
          <a-space direction="vertical" style="width: 100%">
            <a-space>
              <a-button type="primary">按钮1</a-button>
              <a-button>按钮2</a-button>
              <a-button>按钮3</a-button>
            </a-space>
            <a-space :size="32">
              <a-button type="primary">大间距1</a-button>
              <a-button>大间距2</a-button>
            </a-space>
          </a-space>
        </a-card>

        <a-card title="Divider 分割线" class="section-card">
          <div>
            文本内容
            <a-divider />
            <a-divider dashed />
            <a-divider>中间文字</a-divider>
            <a-divider orientation="left">左侧文字</a-divider>
            <a-divider orientation="right">右侧文字</a-divider>
          </div>
        </a-card>

        <!-- 导航组件 -->
        <a-card title="Menu 菜单" class="section-card">
          <a-menu v-model:selectedKeys="selectedMenuKeys" mode="horizontal" :items="menuItems" />
        </a-card>

        <a-card title="Breadcrumb 面包屑" class="section-card">
          <a-breadcrumb>
            <a-breadcrumb-item>首页</a-breadcrumb-item>
            <a-breadcrumb-item><a href="">应用中心</a></a-breadcrumb-item>
            <a-breadcrumb-item>应用列表</a-breadcrumb-item>
            <a-breadcrumb-item>某应用</a-breadcrumb-item>
          </a-breadcrumb>
        </a-card>

        <a-card title="Pagination 分页" class="section-card">
          <a-space direction="vertical" style="width: 100%">
            <a-pagination :total="50" />
            <a-pagination :total="50" show-size-changer show-quick-jumper />
            <a-pagination :total="500" :page-size="20" />
          </a-space>
        </a-card>

        <a-card title="Steps 步骤条" class="section-card">
          <a-steps :current="current" :items="steps" />
          <div class="steps-action" style="margin-top: 24px">
            <a-button type="primary" @click="nextStep" v-if="current < steps.length - 1">
              下一步
            </a-button>
            <a-button @click="prevStep" v-if="current > 0" style="margin-left: 8px">
              上一步
            </a-button>
          </div>
        </a-card>

        <!-- 数据录入组件 -->
        <a-card title="Form 表单" class="section-card">
          <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
            <a-form-item label="输入框">
              <a-input v-model:value="inputValue" placeholder="请输入内容" />
            </a-form-item>
            <a-form-item label="文本域">
              <a-textarea v-model:value="textareaValue" placeholder="请输入内容" :rows="4" />
            </a-form-item>
            <a-form-item label="下拉选择">
              <a-select v-model:value="selectValue" :options="selectOptions" />
            </a-form-item>
            <a-form-item label="级联选择">
              <a-cascader v-model:value="cascaderValue" :options="cascaderOptions" />
            </a-form-item>
            <a-form-item label="日期选择">
              <a-date-picker v-model:value="dateValue" style="width: 100%" />
            </a-form-item>
            <a-form-item label="时间选择">
              <a-time-picker v-model:value="timeValue" style="width: 100%" />
            </a-form-item>
            <a-form-item label="数字输入">
              <a-input-number :min="1" :max="10" style="width: 100%" />
            </a-form-item>
            <a-form-item label="开关">
              <a-switch v-model:checked="switchValue" />
            </a-form-item>
            <a-form-item label="滑块">
              <a-slider v-model:value="sliderValue" />
            </a-form-item>
            <a-form-item label="评分">
              <a-rate v-model:value="rateValue" allow-half />
            </a-form-item>
            <a-form-item label="多选框">
              <a-checkbox-group v-model:value="checkboxValue">
                <a-checkbox value="apple">苹果</a-checkbox>
                <a-checkbox value="pear">梨</a-checkbox>
                <a-checkbox value="orange">橙子</a-checkbox>
              </a-checkbox-group>
            </a-form-item>
            <a-form-item label="单选框">
              <a-radio-group v-model:value="radioValue">
                <a-radio value="a">选项A</a-radio>
                <a-radio value="b">选项B</a-radio>
                <a-radio value="c">选项C</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-form>
        </a-card>

        <a-card title="Upload 上传" class="section-card">
          <a-upload
            v-model:file-list="uploadFileList"
            name="file"
            action="https://www.mocky.io/v2/5cc8019d300000980a055e76"
            :headers="{ authorization: 'authorization-text' }"
          >
            <a-button>
              <upload-outlined></upload-outlined>
              点击上传
            </a-button>
          </a-upload>
        </a-card>

        <a-card title="Transfer 穿梭框" class="section-card">
          <a-transfer
            v-model:target-keys="transferTargetKeys"
            :data-source="transferData"
            :titles="['源列表', '目标列表']"
            :render="(item: any) => item.title"
            @change="handleTransferChange"
          />
        </a-card>

        <!-- 数据展示组件 -->
        <a-card title="Table 表格" class="section-card">
          <a-table :columns="columns" :data-source="dataSource" :pagination="false">
            <template #bodyCell="{ column }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small">编辑</a-button>
                  <a-button type="link" danger size="small">删除</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>

        <a-card title="Tag 标签" class="section-card">
          <a-space wrap>
            <a-tag>默认标签</a-tag>
            <a-tag color="success">成功</a-tag>
            <a-tag color="processing">进行中</a-tag>
            <a-tag color="error">错误</a-tag>
            <a-tag color="warning">警告</a-tag>
            <a-tag color="default">默认</a-tag>
            <a-tag color="#f50">#f50</a-tag>
            <a-tag color="#2db7f5">#2db7f5</a-tag>
            <a-tag color="#87d068">#87d068</a-tag>
            <a-tag color="#108ee9">#108ee9</a-tag>
            <a-tag closable>可关闭</a-tag>
          </a-space>
        </a-card>

        <a-card title="Badge 徽标" class="section-card">
          <a-space :size="32">
            <a-badge :count="5">
              <a-avatar shape="square" size="large" />
            </a-badge>
            <a-badge :count="0" show-zero>
              <a-avatar shape="square" size="large" />
            </a-badge>
            <a-badge :count="99">
              <a-avatar shape="square" size="large" />
            </a-badge>
            <a-badge :count="100">
              <a-avatar shape="square" size="large" />
            </a-badge>
            <a-badge dot>
              <a-avatar shape="square" size="large" />
            </a-badge>
            <a-badge status="success" text="成功" />
            <a-badge status="error" text="错误" />
            <a-badge status="processing" text="进行中" />
          </a-space>
        </a-card>

        <a-card title="Avatar 头像" class="section-card">
          <a-space :size="16">
            <a-avatar>U</a-avatar>
            <a-avatar>USER</a-avatar>
            <a-avatar size="large">L</a-avatar>
            <a-avatar size="small">S</a-avatar>
            <a-avatar shape="square">方</a-avatar>
            <a-avatar :style="{ backgroundColor: '#87d068' }">G</a-avatar>
            <a-avatar :style="{ backgroundColor: '#1890ff' }">
              <template #icon><user-outlined /></template>
            </a-avatar>
          </a-space>
        </a-card>

        <a-card title="Descriptions 描述列表" class="section-card">
          <a-descriptions title="用户信息" bordered>
            <a-descriptions-item label="姓名">张三</a-descriptions-item>
            <a-descriptions-item label="手机号">1810000000</a-descriptions-item>
            <a-descriptions-item label="居住地">浙江杭州</a-descriptions-item>
            <a-descriptions-item label="备注">
              这是一段很长的备注信息，可以包含多行内容。
            </a-descriptions-item>
            <a-descriptions-item label="地址">
              浙江省杭州市西湖区某某街道某某号
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card title="Timeline 时间轴" class="section-card">
          <a-timeline>
            <a-timeline-item>创建服务现场 2015-09-01</a-timeline-item>
            <a-timeline-item>初步排除网络异常 2015-09-01</a-timeline-item>
            <a-timeline-item color="red">
              技术测试异常 2015-09-01
            </a-timeline-item>
            <a-timeline-item>
              网络异常正在修复 2015-09-01
            </a-timeline-item>
          </a-timeline>
        </a-card>

        <a-card title="Tree 树形控件" class="section-card">
          <a-tree
            v-model:checkedKeys="treeCheckedKeys"
            checkable
            :tree-data="treeData"
          />
        </a-card>

        <a-card title="Collapse 折叠面板" class="section-card">
          <a-collapse v-model:activeKey="collapseActiveKey">
            <a-collapse-panel key="1" header="这是面板标题1">
              <p>面板1的内容</p>
            </a-collapse-panel>
            <a-collapse-panel key="2" header="这是面板标题2">
              <p>面板2的内容</p>
            </a-collapse-panel>
            <a-collapse-panel key="3" header="这是面板标题3">
              <p>面板3的内容</p>
            </a-collapse-panel>
          </a-collapse>
        </a-card>

        <a-card title="Tabs 标签页" class="section-card">
          <a-tabs v-model:activeKey="currentTab">
            <a-tab-pane key="1" tab="Tab 1">
              这是第一个标签页的内容
            </a-tab-pane>
            <a-tab-pane key="2" tab="Tab 2">
              这是第二个标签页的内容
            </a-tab-pane>
            <a-tab-pane key="3" tab="Tab 3">
              这是第三个标签页的内容
            </a-tab-pane>
          </a-tabs>
        </a-card>

        <a-card title="Statistic 统计数值" class="section-card">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-statistic title="活跃用户" :value="112893" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="账户余额" :value="112893" :precision="2" prefix="¥" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="增长率" :value="11.28" suffix="%" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="反馈" :value="893" suffix="/ 1000" />
            </a-col>
          </a-row>
        </a-card>

        <a-card title="List 列表" class="section-card">
          <a-list
            :data-source="[
              { title: '列表项目 1' },
              { title: '列表项目 2' },
              { title: '列表项目 3' },
              { title: '列表项目 4' }
            ]"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.title">
                  <template #description>
                    这是列表项的描述内容
                  </template>
                  <template #avatar>
                    <a-avatar>A</a-avatar>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <a-card title="Calendar 日历" class="section-card">
          <a-calendar :fullscreen="false" />
        </a-card>

        <a-card title="Image 图片" class="section-card">
          <a-space>
            <a-image
              width="200"
              src="https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png"
            />
            <a-image
              width="200"
              src="https://gw.alipayobjects.com/zos/antfincdn/LlvErxo8H9/photo-1503185912284-5271ff81b9a8.webp"
            />
          </a-space>
        </a-card>

        <a-card title="Card 卡片" class="section-card">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="卡片标题">
                <p>卡片内容</p>
                <p>卡片内容</p>
                <p>卡片内容</p>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card hoverable>
                <template #cover>
                  <img
                    alt="example"
                    src="https://gw.alipayobjects.com/zos/rmsportal/JiqGstEfoWAOHiTxclqi.png"
                  />
                </template>
                <a-card-meta title="卡片标题" description="卡片描述信息" />
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="带操作的卡片">
                <template #extra>
                  <a href="#">更多</a>
                </template>
                <p>卡片内容</p>
              </a-card>
            </a-col>
          </a-row>
        </a-card>

        <!-- 反馈组件 -->
        <a-card title="Alert 警告提示" class="section-card">
          <a-space direction="vertical" style="width: 100%">
            <a-alert message="成功提示" type="success" />
            <a-alert message="信息提示" type="info" />
            <a-alert message="警告提示" type="warning" />
            <a-alert message="错误提示" type="error" />
            <a-alert
              message="成功提示"
              description="这是成功提示的详细描述"
              type="success"
              closable
            />
            <a-alert
              message="带图标的提示"
              type="info"
              show-icon
              closable
            />
          </a-space>
        </a-card>

        <a-card title="Message & Notification 全局提示" class="section-card">
          <a-space>
            <a-button @click="showMessage">显示 Message</a-button>
            <a-button @click="showNotification">显示 Notification</a-button>
          </a-space>
        </a-card>

        <a-card title="Modal 对话框" class="section-card">
          <a-space>
            <a-button @click="showModal">显示对话框</a-button>
            <a-button
              @click="
                () => {
                  Modal.confirm({
                    title: '确认操作',
                    content: '你确定要执行此操作吗？'
                  })
                }
              "
            >
              确认对话框
            </a-button>
          </a-space>
        </a-card>

        <a-card title="Drawer 抽屉" class="section-card">
          <a-button type="primary">打开抽屉（需要点击触发）</a-button>
        </a-card>

        <a-card title="Progress 进度条" class="section-card">
          <a-space direction="vertical" style="width: 100%">
            <a-progress :percent="30" />
            <a-progress :percent="50" status="active" />
            <a-progress :percent="70" status="exception" />
            <a-progress :percent="100" />
            <a-progress :percent="50" :steps="5" />
            <a-progress type="circle" :percent="75" />
            <a-progress type="circle" :percent="100" />
          </a-space>
        </a-card>

        <a-card title="Spin 加载中" class="section-card">
          <a-space direction="vertical" style="width: 100%">
            <a-spin />
            <a-spin size="small" />
            <a-spin size="large" />
            <a-button @click="toggleSpin">切换加载状态</a-button>
            <a-spin :spinning="spinning">
              <div style="padding: 50px; background: rgba(0, 0, 0, 0.05)">
                这里是内容区域
              </div>
            </a-spin>
          </a-space>
        </a-card>

        <a-card title="Skeleton 骨架屏" class="section-card">
          <a-skeleton active />
          <a-divider />
          <a-skeleton avatar active />
        </a-card>

        <a-card title="Result 结果" class="section-card">
          <a-result
            status="success"
            title="操作成功！"
            sub-title="订单号: 2017182818828182881 将在 2 小时内处理完成"
          >
            <template #extra>
              <a-button type="primary">返回首页</a-button>
              <a-button>查看订单</a-button>
            </template>
          </a-result>
        </a-card>

        <a-card title="Empty 空状态" class="section-card">
          <a-empty />
          <a-divider />
          <a-empty description="暂无数据" />
        </a-card>

        <a-card title="Popover 气泡卡片" class="section-card">
          <a-space>
            <a-popover title="标题" content="这是气泡卡片的内容">
              <a-button>上方</a-button>
            </a-popover>
            <a-popover title="标题" content="这是气泡卡片的内容" placement="bottom">
              <a-button>下方</a-button>
            </a-popover>
            <a-popover title="标题" content="这是气泡卡片的内容" placement="left">
              <a-button>左侧</a-button>
            </a-popover>
            <a-popover title="标题" content="这是气泡卡片的内容" placement="right">
              <a-button>右侧</a-button>
            </a-popover>
          </a-space>
        </a-card>

        <a-card title="Tooltip 文字提示" class="section-card">
          <a-space>
            <a-tooltip title="提示文字">
              <a-button>上方</a-button>
            </a-tooltip>
            <a-tooltip title="提示文字" placement="bottom">
              <a-button>下方</a-button>
            </a-tooltip>
            <a-tooltip title="提示文字" placement="left">
              <a-button>左侧</a-button>
            </a-tooltip>
            <a-tooltip title="提示文字" placement="right">
              <a-button>右侧</a-button>
            </a-tooltip>
          </a-space>
        </a-card>

        <a-card title="Popconfirm 气泡确认框" class="section-card">
          <a-popconfirm
            title="确定要删除这条记录吗？"
            ok-text="确定"
            cancel-text="取消"
          >
            <a-button danger>删除</a-button>
          </a-popconfirm>
        </a-card>

        <!-- 其他组件 -->
        <a-card title="Anchor 锚点" class="section-card">
          <a-anchor>
            <a-anchor-link href="#components-anchor-demo-basic" title="Basic demo" />
            <a-anchor-link href="#components-anchor-demo-static" title="Static demo" />
            <a-anchor-link href="#API" title="API">
              <a-anchor-link href="#Anchor-Props" title="Anchor Props" />
              <a-anchor-link href="#Link-Props" title="Link Props" />
            </a-anchor-link>
          </a-anchor>
        </a-card>

        <a-card title="BackTop 回到顶部" class="section-card">
          <a-back-top />
          <p>这是回到顶部组件，滚动页面后会在右下角显示</p>
        </a-card>

        <a-card title="Affix 固钉" class="section-card">
          <a-affix :offset-top="0">
            <a-button type="primary">固定在顶部</a-button>
          </a-affix>
        </a-card>

        <a-card title="Watermark 水印" class="section-card">
          <a-watermark content="Ant Design Vue">
            <div style="height: 200px; background: rgba(0, 0, 0, 0.02)"></div>
          </a-watermark>
        </a-card>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<style scoped>
.antd-preview {
  min-height: 100vh;
  background: #f0f2f5;
}

.header {
  background: #fff;
  padding: 0 50px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header h1 {
  margin: 0;
  line-height: 64px;
  color: #1890ff;
}

.content {
  padding: 24px 50px;
}

.section-card {
  margin-bottom: 24px;
}

.demo-col {
  padding: 16px 0;
  text-align: center;
  background: #1890ff;
  color: #fff;
  border-radius: 4px;
}

:deep(.ant-card-head) {
  background: #fafafa;
  font-weight: 600;
}
</style>
