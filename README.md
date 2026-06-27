# Iron3Angle-Chat
铁三角 Java 即时通信系统

这是一个基于 Java Socket 的多人聊天项目，包含服务器端、GUI 客户端和控制台客户端。项目支持群聊、私聊、文件发送、昵称修改、在线用户管理以及聊天历史记录。

## 项目特点

- 服务端负责监听客户端连接、维护在线用户、广播消息和转发私聊。
- 客户端支持图形界面和控制台模式。
- 支持群聊、私聊、文件传输、昵称修改。
- 聊天记录和接收文件会保存到项目目录下的 `历史聊天记录/`。

## 目录结构

- `src/common`
  - `ChatMessage.java`：消息模型，定义消息类型、发送者、目标和内容。
  - `MessageUtils.java`：消息协议的格式化与解析工具。
  - `HistoryLogger.java`：保存聊天历史和接收文件。

- `src/server`
  - `Server.java`：服务器主程序。
  - `ClientHandler.java`：处理单个客户端连接的线程逻辑。
  - `GuiServer.java`：服务器控制界面。

- `src/client`
  - `Client.java`：客户端入口，默认启动 GUI 登录界面；如需控制台模式可使用 `--console` 参数。
  - `ClientGuiApp.java`：GUI 客户端主逻辑。
  - `GuiClientMain.java`：登录窗口。
  - `GuiClient.java`：聊天窗口。

## 环境要求

- JDK 8 或更高版本
- 推荐使用 IntelliJ IDEA / VS Code + Java 扩展

## 编译项目

在项目根目录执行：

```sh
javac -d out src/common/*.java src/server/*.java src/client/*.java
```

如果只编译某个模块，例如服务器：

```sh
javac -d out src/common/*.java src/server/*.java
```

## 运行方式

### 1. 启动服务器

```sh
java -cp out server.Server
```

服务端会监听默认端口 `8000`。

### 2. 启动客户端

默认启动 GUI 登录界面：

```sh
java -cp out client.Client
```

如果想进入控制台客户端模式：

```sh
java -cp out client.Client --console
```

### 3. GUI 客户端操作流程

- 输入服务器地址，例如 `127.0.0.1`
- 输入服务器端口，例如 `8000`
- 输入 I3A 账号
- 可选输入昵称
- 点击“登录”后进入聊天窗口

### 4. 控制台客户端支持的命令

- 普通消息：直接输入文本并回车发送群聊
- 私聊：`/pm <目标账号或昵称> <消息>`
- 发送文件：`/file <ALL|目标账号或昵称> <文件路径>`
- 修改昵称：`/nick <新昵称>`
- 退出：`/quit`

## 功能说明

- 登录与在线用户管理
- 群聊与私聊
- 文件发送与接收
- 昵称修改
- 聊天历史与文件保存

## 注意事项

- 默认服务器端口为 `8000`
- GUI 客户端和控制台客户端使用同一套消息协议通信
- 聊天记录与接收文件默认保存在项目根目录下的 `历史聊天记录/` 目录
- 文件发送前会先进行 Base64 编码，接收后会自动保存
