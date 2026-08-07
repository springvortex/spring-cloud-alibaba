# Service Common

公共模块，所有微服务的基础依赖。

## 职责

- 统一响应封装 `ApiResponse`
- 跨服务传输 DTO
- 跨服务共享的 Feign API 契约
- 统一错误码与常量

## 包结构

```
com.zjc.common
├── api
│   ├── mail           MailFeignApi —— 邮件服务共享 Feign 客户端
│   └── test           TestApi —— 连通性测试共享 Feign 客户端
├── constant
│   ├── ApiResponseEnum 响应码标准枚举（实现 ErrorCode 接口）
│   └── ErrorCode       错误码接口
├── dto                跨服务传输对象
│   ├── UserDTO         用户
│   ├── GoodsDTO        商品
│   ├── OrderDTO        订单（含明细列表）
│   ├── OrderDetailDTO  订单明细
│   ├── MailSendDTO     邮件发送请求
│   └── MailLogDTO      邮件发送记录响应
└── web
    └── ApiResponse     统一响应封装
```

## 统一响应

所有接口统一返回 `ApiResponse<T>`，结构：

```json
{
  "success": true,
  "code": 0,
  "message": "操作成功",
  "data": { },
  "timestamp": 1723017600000
}
```

响应码段位规划：

| 码段 | 含义 |
|------|------|
| 0 | 成功 |
| -1 | 通用失败 |
| 1xx | 参数校验类 |
| 4xx | 认证授权类 |
| 5xx | 服务端异常类 |

## 共享 Feign API

common 模块中定义了跨服务共享的 Feign 客户端接口，其他服务引入 common 依赖后可直接注入使用，无需各自重复定义。

| 接口 | 目标服务 | 方法 | 说明 |
|------|----------|------|------|
| `MailFeignApi` | service-mail | `POST /mail/send` | 发送邮件 |
| `TestApi` | service-provider | `GET /port` | 获取 provider 实例端口，验证链路连通性 |

## 依赖

该模块不打包为可执行 Spring Boot 应用，仅作为 jar 供其他模块引入。
源码会通过 `maven-source-plugin` 一并打包，方便其他模块引用时查看源码。
