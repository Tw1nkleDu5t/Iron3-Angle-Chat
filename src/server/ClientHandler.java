package server;

import common.ChatMessage;
import common.HistoryLogger;
import common.MessageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

public class ClientHandler implements Runnable {
    /*
     * 每个 ClientHandler 对应一个客户端连接。
     * 负责读取客户端消息、解析协议、执行业务逻辑、并将响应发送回客户端。
     */
    private final Socket socket;
    private final Server server;
    private PrintWriter writer;
    /* 当前客户端 I3A账号，登录成功后写入 */
    private String username = "";
    /* 可选昵称，用于展示更友好的用户名称 */
    private String nickname = "";
    /* 当前连接是否仍然有效 */
    private boolean connected = true;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    /**
     * 线程启动入口：持续读取客户端发送的每一行协议文本。
     */

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer = new PrintWriter(socket.getOutputStream(), true);
            while (connected) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                /*
                 * 将收到的文本解析为 ChatMessage 对象，再根据消息类型分发处理。
                 */
                ChatMessage message = MessageUtils.parse(line);
                handleMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    /**
     * 统一入口，根据消息类型调用不同的处理逻辑。
     */
    private void handleMessage(ChatMessage message) {
        switch (message.getType()) {
            case LOGIN -> handleLogin(message);
            case LOGOUT -> handleLogout();
            case MESSAGE -> handleBroadcast(message);
            case PRIVATE -> handlePrivate(message);
            case FILE -> handleFile(message);
            case NICKNAME -> handleNickname(message);
            default -> sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", username, "未知消息类型: " + message.getType()));
        }
    }

    private void handleLogin(ChatMessage message) {
        String requestedName = message.getSender();
        /* 登录时将客户端加入服务器维护的在线列表 */
        if (!server.addClient(requestedName, this)) {
            sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", requestedName, "I3A账号已存在或无效，请重新连接。"));
            connected = false;
            closeConnection();
            return;
        }
        username = requestedName;
        sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", username, "您好，I3A账号 " + username + " 连接成功！"));
        server.broadcastSystemMessage("I3A账号 " + username + " 已上线。" );
        server.broadcastUserList();
        /* message.getContent() 可包含客户端本机 IP/port，用于后续同步或日志记录 */
        System.out.println("I3A账号上线: " + username + " 信息:" + message.getContent());
    }

    /**
     * 处理客户端主动断开连接请求。
     * 标记连接已关闭，并通知服务器移除在线用户。
     */
    private void handleLogout() {
        connected = false;
        server.removeClient(username);
    }

    /**
     * 处理群聊消息，将消息广播给所有在线客户端。
     * 同时将聊天内容写入历史日志，便于后续查看。
     */
    private void handleBroadcast(ChatMessage message) {
        if (username.isBlank()) {
            sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", "", "请先登录。"));
            return;
        }
        String text = username + ": " + message.getContent();
        HistoryLogger.log("MESSAGE: " + text);
        server.broadcastMessage(new ChatMessage(ChatMessage.Type.MESSAGE, username, "ALL", message.getContent()));
    }

    /**
     * 处理私聊消息。若目标为 ALL，则退化为广播。
     * 否则尝试发送给指定目标用户，并反馈目标是否存在。
     */
    private void handlePrivate(ChatMessage message) {
        if (username.isBlank()) {
            sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", "", "请先登录。"));
            return;
        }
        ChatMessage privateMessage = new ChatMessage(ChatMessage.Type.PRIVATE, username, message.getTarget(), message.getContent());
        if (message.getTarget().isBlank() || message.getTarget().equalsIgnoreCase("ALL")) {
            HistoryLogger.log("PRIVATE-BROADCAST: " + username + " -> ALL: " + message.getContent());
            server.broadcastMessage(privateMessage);
        } else {
            boolean sent = server.sendPrivateMessage(message.getTarget(), privateMessage);
            if (sent) {
                HistoryLogger.log("PRIVATE: " + username + " -> " + message.getTarget() + ": " + message.getContent());
            } else {
                sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", username, "目标用户不存在: " + message.getTarget()));
            }
        }
    }

    /**
     * 处理文件传输消息。
     * 文件内容以 Base64 编码后作为 content 发送，正文格式为 fileName###base64Data。
     */
    private void handleFile(ChatMessage message) {
        if (username.isBlank()) {
            sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", "", "请先登录。"));
            return;
        }
        String[] parts = message.getContent().split("###", 2);
        if (parts.length != 2) {
            sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", username, "文件消息格式错误。"));
            return;
        }
        String fileName = parts[0];
        byte[] data = Base64.getDecoder().decode(parts[1]);
        if (message.getTarget().isBlank() || message.getTarget().equalsIgnoreCase("ALL")) {
            HistoryLogger.log("FILE: " + username + " -> ALL: " + fileName);
            server.broadcastMessage(new ChatMessage(ChatMessage.Type.FILE, username, "ALL", message.getContent()));
        } else {
            boolean sent = server.sendPrivateMessage(message.getTarget(), new ChatMessage(ChatMessage.Type.FILE, username, message.getTarget(), message.getContent()));
            if (sent) {
                HistoryLogger.log("FILE: " + username + " -> " + message.getTarget() + ": " + fileName);
            } else {
                sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", username, "目标用户不存在: " + message.getTarget()));
            }
        }
    }

    /**
     * 处理客户端昵称修改请求。
     * 更新昵称后通知客户端并刷新所有用户列表。
     */
    private void handleNickname(ChatMessage message) {
        if (username.isBlank()) {
            sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", "", "请先登录。"));
            return;
        }
        nickname = message.getContent();
        HistoryLogger.log("NICKNAME: " + username + " -> " + nickname);
        sendMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", username, "昵称已更新为: " + nickname));
        server.broadcastUserList();
    }

    /**
     * 发送消息给当前客户端。
     * 这里使用 MessageUtils.format 将 ChatMessage 转换为统一协议字符串。
     */
    public void sendMessage(ChatMessage message) {
        if (writer != null) {
            writer.println(MessageUtils.format(message));
        }
    }

    /**
     * 关闭客户端连接，释放资源。
     */
    public void closeConnection() {
        connected = false;
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 获取客户端绑定的 I3A 账号。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取客户端设置的昵称。
     */
    public String getNickname() {
        return nickname;
    }
}
