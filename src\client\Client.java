package client;

import common.ChatMessage;
import common.MessageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Scanner;
import javax.swing.SwingUtilities;

public class Client {
    /*
     * 控制台客户端主类，负责与服务器建立连接并发送命令。
     * 本客户端使用纯文本命令交互，适合命令行演示。
     */
    private final String host;
    private final int port;
    private final String username;
    private Socket socket;
    private PrintWriter writer;

    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && "--console".equalsIgnoreCase(args[0])) {
            runConsoleClient();
            return;
        }

        SwingUtilities.invokeLater(ClientGuiApp::new);
    }

    private static void runConsoleClient() {
        /*
         * 启动入口：从控制台读取服务器地址、端口和用户名。
         * 这部分代码用于演示如何初始化客户端连接参数。
         */
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入服务器地址（默认 localhost）:");
        String host = scanner.nextLine().trim();
        if (host.isBlank()) {
            host = "localhost";
        }
        System.out.println("请输入服务器端口（默认 8000）:");
        String portValue = scanner.nextLine().trim();
        int port = 8000;
        if (!portValue.isBlank()) {
            try {
                port = Integer.parseInt(portValue);
            } catch (NumberFormatException ignored) {
            }
        }
        System.out.println("请输入你的I3A账号:");
        String username = scanner.nextLine().trim();
        if (username.isBlank()) {
            System.out.println("I3A账号不能为空。");
            return;
        }
        Client client = new Client(host, port, username);
        client.start();
    }

    /**
     * 客户端启动流程：建立 socket 连接、登录发送用户名、启动接收线程、进入用户输入循环。
     */
    public void start() {
        try {
            /* 创建 TCP socket，连接到服务器 */
            socket = new Socket(host, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            /* 登录时发送 I3A 账号 和本机地址，服务器可用于识别客户端 */
            String localInfo = InetAddress.getLocalHost().getHostAddress() + ":" + socket.getLocalPort();
            writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.LOGIN, username, "ALL", localInfo)));
            new Thread(this::readMessages).start();
            sendUserInput();
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
        }
    }

    /**
     * 读取命令行输入并转换为对应协议消息。
     * 支持群聊、私聊、文件发送、昵称修改和退出命令。
     */
    private void sendUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入消息并回车发送。\n"
                + "- /pm 目标账号或昵称 消息：进行私聊\n"
                + "- /file ALL|目标账号或昵称 文件路径：发送文件\n"
                + "- /nick 新昵称：修改昵称\n"
                + "- /quit：退出聊天\n");
        while (socket != null && !socket.isClosed()) {
            String line = scanner.nextLine();
            if (line == null || line.isBlank()) {
                continue;
            }
            if (line.equalsIgnoreCase("/quit")) {
                writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.LOGOUT, username, "ALL", "")));
                break;
            }
            if (line.startsWith("/nick ")) {
                String[] parts = line.split(" ", 2);
                if (parts.length < 2 || parts[1].isBlank()) {
                    System.out.println("格式: /nick 新昵称");
                    continue;
                }
                writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.NICKNAME, username, "ALL", parts[1].trim())));
                continue;
            }
            if (line.startsWith("/file ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length < 3) {
                    System.out.println("格式: /file ALL 文件路径 或 /file 目标账号或昵称 文件路径");
                    continue;
                }
                String target = parts[1].trim();
                Path path = Path.of(parts[2].trim());
                if (!Files.exists(path)) {
                    System.out.println("文件不存在: " + path);
                    continue;
                }
                try {
                    byte[] bytes = Files.readAllBytes(path);
                    String encoded = Base64.getEncoder().encodeToString(bytes);
                    String content = path.getFileName() + "###" + encoded;
                    writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.FILE, username, target, content)));
                } catch (IOException e) {
                    System.out.println("文件读取失败: " + e.getMessage());
                }
                continue;
            }
            if (line.startsWith("/pm ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length < 3) {
                    System.out.println("格式: /pm 目标账号或昵称 消息");
                    continue;
                }
                writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.PRIVATE, username, parts[1], parts[2])));
            } else {
                writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.MESSAGE, username, "ALL", line)));
            }
        }
        closeConnection();
        System.out.println("已退出。" );
    }

    /**
     * 后台线程负责持续读取服务器推送的消息，并根据类型输出不同格式。
     */
    private void readMessages() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ChatMessage message = MessageUtils.parse(line);
                /* 根据不同消息类型展示不同提示 */
                switch (message.getType()) {
                    case SYSTEM -> System.out.println("[系统] " + message.getContent());
                    case USERS -> System.out.println("[在线I3A账号] " + message.getContent());
                    case PRIVATE -> System.out.println("[私聊] " + message.getSender() + " -> " + message.getTarget() + ": " + message.getContent());
                    case MESSAGE -> System.out.println("[群聊] " + message.getSender() + ": " + message.getContent());
                    case FILE -> handleIncomingFile(message);
                    case NICKNAME -> System.out.println("[昵称] " + message.getSender() + " 已将昵称设置为: " + message.getContent());
                    default -> System.out.println("[未知] " + message.getContent());
                }
            }
        } catch (SocketException ignored) {
        } catch (IOException e) {
            System.err.println("读取服务器消息失败: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    /**
     * 关闭 socket 连接并释放客户端资源。
     */
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 处理来自服务器的文件消息：解析文件名和 Base64 内容，保存为本地文件。
     */
    private void handleIncomingFile(ChatMessage message) {
        String[] parts = message.getContent().split("###", 2);
        if (parts.length != 2) {
            System.out.println("[文件] 收到格式错误的文件消息。");
            return;
        }
        String fileName = parts[0];
        byte[] data = Base64.getDecoder().decode(parts[1]);
        String savedPath = common.HistoryLogger.saveReceivedFile(fileName, data);
        if (savedPath != null) {
            System.out.println("[文件] 从 " + message.getSender() + " 收到文件: " + fileName + "，已保存到 " + savedPath);
        } else {
            System.out.println("[文件] 收到文件: " + fileName + "，但保存失败。");
        }
    }
}
