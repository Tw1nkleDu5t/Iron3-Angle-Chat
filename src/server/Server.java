package server;

import common.ChatMessage;
import common.HistoryLogger;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Server {
    /*
     * 服务器主入口，管理客户端连接、在线用户和消息转发。
     * 通过 GUI 界面控制服务器启动和关闭。
     */
    private static final Path PID_FILE = Paths.get("server.pid");

    private ServerSocket serverSocket;
    private GuiServer guiServer;
    private int port = Integer.getInteger("server.port", 8000);
    private boolean running = false;
    /*
     * 保存在线客户端处理器，key 为 I3A 账号，value 为对应的 ClientHandler。
     * 使用 synchronizedMap 保证多线程场景下的线程安全访问。
     */
    Map<String, ClientHandler> clientHandlers = Collections.synchronizedMap(new HashMap<>());

    /**
     * 初始化服务器 GUI 界面并绑定按钮事件。
     */
    public Server() {
        guiServer = new GuiServer();
        guiServer.startButListenter(this);
        guiServer.endButtonListenter(this);
    }

    /**
     * 启动服务器监听线程，接受客户端连接请求。
     * 端口固定为 8000，也是客户端默认连接端口。
     */
    public void startServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            guiServer.appendLog("服务器已经在运行，端口 " + port);
            return;
        }

        try {
            bindServerSocket();
            running = true;
            new Thread(new StartServerThread()).start();
            guiServer.appendLog("服务器已启动，监听端口 " + port);
            guiServer.setStartButtonEnabled(false);
            guiServer.setCloseButtonEnabled(true);
        } catch (IOException e) {
            running = false;
            serverSocket = null;
            guiServer.setStartButtonEnabled(true);
            guiServer.setCloseButtonEnabled(false);
            guiServer.appendLog("服务器启动失败：" + e.getMessage());
            if (e instanceof BindException) {
                guiServer.appendLog("端口 " + port + " 仍被占用，无法自动释放旧实例。请稍后重试或修改端口。" );
            }
        }
    }

    /**
     * 关闭服务器监听并退出程序。
     * 该方法会结束服务器主线程，断开所有后续连接请求。
     */
    public void endServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        serverSocket = null;
        removePidFile();
        guiServer.setStartButtonEnabled(true);
        guiServer.setCloseButtonEnabled(false);
        guiServer.appendLog("服务器已关闭。");
        System.exit(0);
    }

    private void bindServerSocket() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            writePidFile();
        } catch (BindException e) {
            if (tryStopPreviousInstance()) {
                guiServer.appendLog("已尝试关闭旧服务器实例，正在重新绑定端口 " + port + "...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                serverSocket = new ServerSocket(port);
                writePidFile();
            } else {
                throw e;
            }
        }
    }

    private void writePidFile() {
        try {
            Files.write(
                    PID_FILE,
                    String.valueOf(ProcessHandle.current().pid()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            guiServer.appendLog("无法写入进程标识文件：" + e.getMessage());
        }
    }

    private void removePidFile() {
        try {
            Files.deleteIfExists(PID_FILE);
        } catch (IOException ignored) {
        }
    }

    private boolean tryStopPreviousInstance() {
        if (!Files.exists(PID_FILE)) {
            return false;
        }

        try {
            String pidText = Files.readString(PID_FILE).trim();
            long pid = Long.parseLong(pidText);
            Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isPresent()) {
                ProcessHandle process = handle.get();
                if (process.isAlive()) {
                    process.destroy();
                    try {
                        process.onExit().toCompletableFuture().get(2, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                    }
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                    guiServer.appendLog("已尝试关闭旧服务器进程 PID " + pid);
                    return true;
                }
            }
        } catch (Exception e) {
            guiServer.appendLog("自动关闭旧服务端失败：" + e.getMessage());
        }
        return false;
    }

    /*
     * 接受客户端连接的后台线程实现。
     * accept() 方法会阻塞直到有客户端连接请求到达。
     */
    class StartServerThread implements Runnable {
        @Override
        public void run() {
            while (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket, Server.this)).start();
                } catch (IOException e) {
                    guiServer.appendLog("服务器停止监听：" + e.getMessage());
                }
            }
        }
    }

    /**
     * 将新客户端加入在线列表。
     * 如果账号为空、重复或已存在，则拒绝登录。
     */
    public boolean addClient(String name, ClientHandler handler) {
        if (name == null || name.isBlank() || clientHandlers.containsKey(name)) {
            return false;
        }
        clientHandlers.put(name, handler);
        broadcastUserList();
        return true;
    }

    /**
     * 将客户端从在线列表中移除，广播离线通知并更新所有客户端的在线列表。
     */
    public void removeClient(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        clientHandlers.remove(name);
        broadcastSystemMessage("I3A账号 " + name + " 已下线。");
        broadcastUserList();
    }

    /**
     * 将消息发送给所有在线客户端。
     * 使用 synchronized(clientHandlers) 来保护并发遍历。
     */
    public void broadcastMessage(ChatMessage message) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers.values()) {
                handler.sendMessage(message);
            }
        }
    }

    /**
     * 发送私聊消息给指定目标用户。
     * 返回是否成功找到目标客户端并发送。
     */
    public boolean sendPrivateMessage(String target, ChatMessage message) {
        if (target == null || target.isBlank()) {
            return false;
        }

        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers.values()) {
                String username = handler.getUsername();
                String nickname = handler.getNickname();
                boolean matchesUsername = username != null && username.equalsIgnoreCase(target);
                boolean matchesNickname = nickname != null && nickname.equalsIgnoreCase(target);
                if (matchesUsername || matchesNickname) {
                    handler.sendMessage(message);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 广播系统消息到所有客户端，并将该事件写入历史日志。
     */
    public void broadcastSystemMessage(String text) {
        HistoryLogger.log("SYSTEM: " + text);
        broadcastMessage(new ChatMessage(ChatMessage.Type.SYSTEM, "Server", "ALL", text));
    }

    /**
     * 构建在线用户列表并广播给所有客户端。
     * 这里同时显示账号与昵称，便于客户端显示更友好的用户信息。
     */
    public void broadcastUserList() {
        StringBuilder builder = new StringBuilder();
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers.values()) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                String username = handler.getUsername();
                String nickname = handler.getNickname();
                if (nickname != null && !nickname.isBlank() && !nickname.equals(username)) {
                    builder.append(username).append(" (").append(nickname).append(")");
                } else {
                    builder.append(username);
                }
            }
        }
        broadcastMessage(new ChatMessage(ChatMessage.Type.USERS, "Server", "ALL", builder.toString()));
    }

    public static void main(String[] args) {
        /*
         * 程序入口：创建 Server 实例并自动启动监听。
         * 这样客户端测试时无需手动点击 GUI 按钮即可连接。
         */
        Server server = new Server();
        server.startServer();
    }

}
