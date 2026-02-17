## 目录结构

```
router/
├── index.ts              # 路由主入口
├── types.ts              # 路由类型定义
├── constants.ts          # 路由常量（路由名称、路径、白名单）
├── guards.ts             # 路由守卫（权限控制、页面标题等）
├── utils.ts              # 路由工具函数
├── modules/              # 路由模块
│   ├── index.ts          # 模块导出
│   ├── auth.ts           # 认证相关路由
│   ├── error.ts          # 错误页面路由
│   └── ...               # 其他业务模块路由
└── README.md             # 使用说明
```

## 核心特性

### 1. 模块化管理
- 按业务模块拆分路由配置
- 便于维护和扩展
- 支持路由懒加载

### 2. 类型安全
- 完整的 TypeScript 类型定义
- 路由元信息类型化
- 编译时错误检测

### 3. 路由守卫
- 登录状态检查
- 权限验证
- 页面标题设置
- 进度条显示

### 4. 路由常量
- 统一管理路由名称和路径
- 避免硬编码
- 便于重构

### 5. 工具函数
- 面包屑生成
- 路由过滤
- 路径解析

## 使用示例

### 添加新路由模块

```typescript
// router/modules/dashboard.ts
import type { AppRouteRecordRaw } from '../types'
import { RouteNames, RoutePaths } from '../constants'

const dashboardRoutes: AppRouteRecordRaw[] = [
  {
    path: RoutePaths.DASHBOARD,
    name: RouteNames.DASHBOARD,
    component: () => import('@/pages/Dashboard.vue'),
    meta: {
      title: '仪表盘',
      requiresAuth: true,
      icon: 'dashboard',
    },
  },
]

export default dashboardRoutes
```

### 路由导航

```typescript
import { useRouter } from 'vue-router'
import { RouteNames, RoutePaths } from '@/router'

const router = useRouter()

// 使用路由名称
router.push({ name: RouteNames.DASHBOARD })

// 使用路由路径
router.push(RoutePaths.DASHBOARD)
```

### 权限控制

```typescript
// 在路由 meta 中定义角色
meta: {
  requiresAuth: true,
  roles: [Role.ADMIN, Role.EDIT],
}
```

### 路由元信息

```typescript
interface RouteMeta {
  title?: string              // 页面标题
  icon?: string              // 图标
  requiresAuth?: boolean     // 是否需要认证
  roles?: Role[]             // 角色权限
  hidden?: boolean           // 是否在菜单中隐藏
  keepAlive?: boolean        // 是否缓存页面
  breadcrumb?: boolean       // 是否显示面包屑
  activeMenu?: string        // 激活的菜单路径
  affix?: boolean            // 是否固定在标签栏
  externalLink?: string      // 外链地址
}
```

## 最佳实践

1. **始终使用路由常量**：避免在代码中硬编码路由路径
2. **模块化拆分**：按业务模块拆分路由配置
3. **懒加载组件**：使用动态 import 实现路由懒加载
4. **类型化 meta**：充分利用 TypeScript 类型系统
5. **守卫解耦**：路由守卫逻辑应与业务逻辑解耦

## 扩展功能

### 动态路由
可以根据用户权限动态添加路由：

```typescript
import { router } from '@/router'

function addDynamicRoutes(routes: RouteRecordRaw[]) {
  routes.forEach((route) => {
    router.addRoute(route)
  })
}
```

### 路由缓存
利用 `keepAlive` meta 属性实现页面缓存：

```vue
<router-view v-slot="{ Component, route }">
  <keep-alive>
    <component :is="Component" :key="route.path" v-if="route.meta.keepAlive" />
  </keep-alive>
  <component :is="Component" :key="route.path" v-if="!route.meta.keepAlive" />
</router-view>
```

## 注意事项

1. 路由守卫中的异步操作需要正确处理错误
2. 动态路由需要在刷新时重新添加
3. 路由缓存需要配合 keep-alive 使用
4. 外链跳转需要特殊处理
