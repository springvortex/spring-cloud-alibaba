# MP-Generator（MyBatis-Plus 代码生成器）

基于 [MyBatis-Plus](https://baomidou.com/) 3.5.17 的 `FastAutoGenerator`，连接数据库后自动生成 Entity、Mapper、Service、ServiceImpl、Mapper XML 等代码。所有可变配置外置到 `generator.properties`，换库、换表只需改配置文件，无需改动 Java 代码。

## 环境要求

- JDK 21
- Maven 3.6+
- 可访问的 MySQL 数据库

## 目录结构

```
MP-Generator/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/zjc/CodeGenerator.java   # 生成器入口
    └── resources/generator.properties     # 生成配置
```

## 配置说明

编辑 [`src/main/resources/generator.properties`](src/main/resources/generator.properties)：

| 配置项 | 说明 | 示例 |
| --- | --- | --- |
| `db.url` | 数据库 JDBC 连接地址 | `jdbc:mysql://host:3306/db?...` |
| `db.username` | 数据库用户名 | `root` |
| `db.password` | 数据库密码 | `123456` |
| `package.parent` | 生成代码的父包名 | `com.zjc.provider` |
| `generator.tables` | 需要生成的表，逗号分隔 | `t_user,t_order` |
| `generator.tablePrefix` | 表前缀（生成类名时去掉），逗号分隔 | `t_,sys_` |

## 运行方式

### 方式一：IntelliJ IDEA

1. 用 IDEA 打开本模块，等待 Maven 导入完成。
2. 打开 `src/main/java/com/zjc/CodeGenerator.java`。
3. 右键运行 `main` 方法（`Run 'CodeGenerator.main()'`）。

### 方式二：命令行

```bash
# 编译
mvn compile
# 运行（需确保 target/classes 已生成）
mvn exec:java -Dexec.mainClass="com.zjc.CodeGenerator"
```

## 生成内容

对每张表生成以下文件（已禁用 Controller）：

- `entity/` — 实体类（Lombok `@Data`、字段 `@TableField`、`@Serial serialVersionUID`）
- `mapper/` — Mapper 接口（文件名以 `Mapper` 结尾）
- `service/` — Service 接口（文件名以 `Service` 结尾）
- `service/impl/` — Service 实现类
- `resources/mapper/` — Mapper XML（含 `BaseResultMap`、`BaseColumnList`）

生成的代码输出到本模块的 `src/main/java` 下，目录根据 `package.parent` 自动组织：

```
src/main/java/com/zjc/provider/
├── entity/User.java
├── mapper/UserMapper.java
├── service/UserService.java
└── service/impl/UserServiceImpl.java
src/main/resources/mapper/UserMapper.xml
```

## 输出目录说明

输出目录由 `CodeGenerator.class` 的编译位置（`target/classes`）反推模块根目录得出，因此**无论从哪个工作目录运行**（父工程或本模块），代码都会稳定生成到本模块的 `src/main/java` 下，不会跑到同级目录。

## 常见问题

### 1. 生成的 Service/ServiceImpl 编译报错

生成的 Service 层代码依赖 `mybatis-plus-extension`（`IService` / `ServiceImpl`）和 `org.springframework`，这些是**业务模块**的依赖，而非生成器模块的。生成后请将代码迁移到实际业务模块中使用，不要在生成器模块内编译它们。

### 2. 配置文件中文乱码

`generator.properties` 必须以 **UTF-8** 编码保存。代码已用 `InputStreamReader(..., UTF_8)` 读取，避免 `Properties.load()` 默认 ISO-8859-1 导致的乱码。

## 自定义生成规则

如需调整生成策略（如开启 Controller、修改命名规则、添加表填充字段等），编辑 [`CodeGenerator.java`](src/main/java/com/zjc/CodeGenerator.java) 中的 `strategyConfig` 部分。常用配置项参见 [MyBatis-Plus 官方文档](https://baomidou.com/guides/new-code-generator/)。
