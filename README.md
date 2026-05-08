# 超市管理系统

基于 Spring Boot 3 + Vue 3 的前后端分离超市管理系统，支持多门店运营，集成 AI 智能采购建议。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.5.11 | 核心框架 |
| Spring Security | 6.x | 认证与授权 |
| MyBatis-Plus | 3.5.12 | ORM 框架 |
| MySQL | 8.x | 主数据库 |
| Redis | 7.x | 缓存与会话 |
| JWT (jjwt) | 0.12.3 | Token 认证 |
| Spring AI | 1.0.0-M5 | AI 集成（DeepSeek） |
| Knife4j | 4.3.0 | API 文档 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.29 | 核心框架 |
| Vue Router | 5.0.3 | 路由管理 |
| Pinia | 3.0.4 | 状态管理 |
| Element Plus | 2.13.2 | UI 组件库 |
| ECharts | 6.0.0 | 数据可视化 |
| Axios | 1.13.6 | HTTP 客户端 |
| Vite | 6.x | 构建工具 |

## 功能模块

- **收银台**：条码扫描、购物车管理、会员折扣、积分累计
- **商品管理**：商品信息维护、分类管理、门店商品配置
- **库存管理**：实时库存跟踪、库存流水、门店间调拨、Redis 双层缓存
- **采购管理**：采购订单创建 → 提交审核 → 审批 → 入库全流程，AI 智能补货建议
- **会员管理**：会员注册、等级体系、积分管理、消费统计
- **统计报表**：销售数据看板、ECharts 可视化

## 项目结构

```
fuben/
└── super/
    ├── supermarket/               # Spring Boot 后端
    │   └── src/main/java/org/supermarket/
    │       ├── common/            # 公共基础设施（配置、工具、异常、响应封装）
    │       └── modules/
    │           ├── product/       # 商品模块
    │           ├── sale/          # 销售模块
    │           ├── purchase/      # 采购模块
    │           ├── inventory/     # 库存模块
    │           ├── member/        # 会员模块
    │           └── stats/         # 统计模块
    └── vue/supermarket-web/       # Vue 3 前端
        └── src/
            ├── views/             # 页面组件
            ├── api/               # HTTP 接口层
            ├── stores/            # Pinia 状态
            ├── router/            # 路由配置
            └── utils/             # 工具函数
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20.19.0 或 >= 22.12.0
- MySQL 8.x
- Redis 7.x

### 后端启动

**1. 初始化数据库**

创建数据库并导入 SQL 脚本：

```sql
CREATE DATABASE supermarket DEFAULT CHARACTER SET utf8mb4;
```

**2. 修改配置**

编辑 `super/supermarket/src/main/resources/application.yml`，根据本地环境调整数据库和 Redis 连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/supermarket
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
```

**3. 启动服务**

```bash
cd super/supermarket
./mvnw spring-boot:run
```

服务启动后访问：
- 后端接口：http://localhost:8080
- API 文档：http://localhost:8080/doc.html

### 前端启动

```bash
cd super/vue/supermarket-web
npm install
npm run dev
```

前端地址：http://localhost:5173

## 核心设计

### 库存并发安全

库存采用 **MySQL 乐观锁 + Redis 缓存** 双层架构：
- 扣减优先操作 Redis，保证高并发下的响应速度
- MySQL `@Version` 乐观锁防止并发超卖
- 散装称重商品支持小数精度（×1000 整数存储）

### 采购审批流

```
草稿(0) → 待审核(1) → 已审批(2) → 已入库(3)
                              ↘ 已取消(4)
```

### AI 智能采购建议

集成 DeepSeek API，根据历史销售数据和当前库存自动推荐最优采购数量。

### 会员折扣体系

销售下单时根据会员等级动态应用折扣率，同步累计消费金额与积分。

## 安全配置

- JWT Token 有效期 24 小时
- Spring Security 无状态会话（STATELESS）
- 跨域（CORS）已配置，允许前端访问
- 公开接口白名单：`/auth/login`、`/doc.html`、`/v3/api-docs/**`
