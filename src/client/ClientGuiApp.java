package client;

import common.ChatMessage;
import common.MessageUtils;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Base64;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientGuiApp {
    /*
     * GUI 客户端主逻辑，负责连接服务器、发送消息、接收服务器推送并更新界面。
     */
    GuiClientMain guiMain;
    GuiClient gui;
    Socket client;
    PrintWriter writer;
    String I3AAccount;
    String clientIp;
    int clientPort;

    /**
     * 构造函数：创建登录窗口并绑定按钮事件。
     */
    public ClientGuiApp() {
        guiMain = new GuiClientMain("Iron3Angle I3A 登录");
        guiMain.listenLoginButton(this);
        guiMain.listenQuitButton(this);
        guiMain.showFrame();
    }

    /**
     * 登录方法：读取登录窗体输入，连接服务器，发送登录消息，并在登录成功后打开主聊天界面。
     */
    public void login() {
        try {
            String serverIP = guiMain.getAddress();
            int serverPort = guiMain.getPort();
            I3AAccount = guiMain.getI3AAccount();
            if (I3AAccount == null || I3AAccount.isBlank()) {
                return;
            }

            // 连接服务器 Socket，完成 TCP 三次握手
            client = new Socket(serverIP, serverPort);
            writer = new PrintWriter(client.getOutputStream(), true);

            /* 获取本机 IP 和端口，登录时发送账号信息 */
            clientIp = client.getLocalAddress().getHostAddress();
            clientPort = client.getLocalPort();
            String localInfo = clientIp + ":" + clientPort;

            ChatMessage loginMessage = new ChatMessage(ChatMessage.Type.LOGIN, I3AAccount, "ALL", localInfo);
            writer.println(MessageUtils.format(loginMessage));

            String nickname = guiMain.getNickname();
            if (nickname != null && !nickname.isBlank()) {
                writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.NICKNAME, I3AAccount, "ALL", nickname)));
            }

            /* 登录成功后关闭登录窗口并显示聊天窗口 */
            guiMain.dispose();
            gui = new GuiClient(I3AAccount);
            gui.listenSendButton(this);
            gui.listenFileButton(this);
            gui.listenCloseButton(this);
            gui.listenNickButton(this);
            gui.showFrame();
            gui.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    quit();
                }
            });

            /* 启动后台线程持续接收服务器推送消息 */
            new Thread(new AcceptMessageThread()).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            if (guiMain != null) {
                guiMain.setTitle("连接失败，请检查服务器");
            }
        }
    }

    /**
     * 发送文本消息：根据当前选择的目标，决定是群聊还是私聊。
     */
    public void send() {
        String sendMsg = gui.getSendMsg();
        if (sendMsg == null || sendMsg.isBlank()) {
            return;
        }
        gui.setSendMsgClear();
        String choiceText = gui.getChoiceText().trim();
        String target = getTargetFromChoice(choiceText);
        ChatMessage message;

        if ("ALL".equalsIgnoreCase(target)) {
            message = new ChatMessage(ChatMessage.Type.MESSAGE, I3AAccount, "ALL", sendMsg);
        } else {
            message = new ChatMessage(ChatMessage.Type.PRIVATE, I3AAccount, target, sendMsg);
        }
        writer.println(MessageUtils.format(message));
    }

    /**
     * 向服务器发送昵称修改请求。
     * GUI 中显示修改框，用户输入完成后点击按钮触发。
     */
    public void sendNickname() {
        String nickname = gui.getNicknameText();
        if (nickname == null || nickname.isBlank()) {
            gui.setReceiveMsg("[系统] 昵称不能为空。 ");
            return;
        }
        writer.println(MessageUtils.format(new ChatMessage(ChatMessage.Type.NICKNAME, I3AAccount, "ALL", nickname.trim())));
    }

    /**
     * 选择文件并发送到服务器。
     * 文件内容会被 Base64 编码后封装进消息协议中。
     */
    public void sendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(gui) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null || !file.exists()) {
            return;
        }

        String choiceText = gui.getChoiceText().trim();
        String target = getTargetFromChoice(choiceText);
        String type = "ALL".equalsIgnoreCase(target) ? "ALL" : target;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String encoded = Base64.getEncoder().encodeToString(bytes);
            String content = file.getName() + "###" + encoded;
            ChatMessage message = new ChatMessage(ChatMessage.Type.FILE, I3AAccount, type, content);
            writer.println(MessageUtils.format(message));
            gui.setReceiveMsg("[文件] 已发送: " + file.getName());
        } catch (IOException e) {
            gui.setReceiveMsg("[文件] 发送失败: " + e.getMessage());
        }
    }

    /**
     * 从用户选择的下拉项中提取目标账号。
     * 如果列表项是 "账号 (昵称)" 格式，则只取账号部分。
     */
    private String getTargetFromChoice(String choiceText) {
        if (choiceText == null || choiceText.isBlank() || "ALL".equalsIgnoreCase(choiceText)) {
            return "ALL";
        }
        int idx = choiceText.indexOf(" (");
        return idx == -1 ? choiceText : choiceText.substring(0, idx);
    }

    /**
     * 退出聊天：发送登出消息给服务器并结束程序。
     */
    public void quit() {
        try {
            ChatMessage logoutMessage = new ChatMessage(ChatMessage.Type.LOGOUT, I3AAccount, "ALL", "");
            writer.println(MessageUtils.format(logoutMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    class AcceptMessageThread implements Runnable {
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
                String str;
                while ((str = reader.readLine()) != null) {
                    ChatMessage message = MessageUtils.parse(str);
                    switch (message.getType()) {
                        case USERS -> updateGui(() -> {
                            String content = message.getContent();
                            if (content.isBlank()) {
                                gui.setChoiceItems(new String[]{"ALL"});
                            } else {
                                String[] users = content.split(",");
                                String[] items = new String[users.length + 1];
                                items[0] = "ALL";
                                for (int i = 0; i < users.length; i++) {
                                    items[i + 1] = users[i].trim();
                                }
                                gui.setChoiceItems(items);
                            }
                        });
                        case MESSAGE -> updateGui(() -> gui.setReceiveMsg("[群聊] " + message.getSender() + ": " + message.getContent()));
                        case PRIVATE -> updateGui(() -> gui.setReceiveMsg("[私聊] " + message.getSender() + " -> 你: " + message.getContent()));
                        case FILE -> handleIncomingFile(message);
                        case NICKNAME -> updateGui(() -> gui.setReceiveMsg("[昵称] " + message.getSender() + " 已将昵称设置为: " + message.getContent()));
                        case SYSTEM -> updateGui(() -> gui.setReceiveMsg("[系统] " + message.getContent()));
                        default -> updateGui(() -> gui.setReceiveMsg("[未知] " + message.getContent()));
                    }
                }
            } catch (IOException e) {
                updateGui(() -> gui.setReceiveMsg("服务器连接断开！"));
            }
        }

        private void updateGui(Runnable task) {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        }

        private void handleIncomingFile(ChatMessage message) {
            String[] parts = message.getContent().split("###", 2);
            if (parts.length != 2) {
                gui.setReceiveMsg("[文件] 收到格式错误的文件消息。");
                return;
            }
            String fileName = parts[0];
            byte[] data = Base64.getDecoder().decode(parts[1]);
            String savedPath = common.HistoryLogger.saveReceivedFile(fileName, data);
            if (savedPath != null) {
                gui.setReceiveMsg("[文件] 从 " + message.getSender() + " 收到文件: " + fileName + "，已保存到 " + savedPath);
            } else {
                gui.setReceiveMsg("[文件] 收到文件: " + fileName + "，但保存失败。");
            }
        }
    }

    public static void main(String[] args) {
        /*
         * GUI 客户端程序入口：启动登录窗口并等待用户操作。
         */
        new ClientGuiApp();
    }
}
