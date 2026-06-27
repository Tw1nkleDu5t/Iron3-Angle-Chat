package server;

import common.ChatMessage;

public class ServerPrivateMessageTest {
    public static void main(String[] args) {
        Server server = new Server();
        RecordingHandler handler = new RecordingHandler(server, "root", "Root");
        if (!server.addClient("root", handler)) {
            throw new AssertionError("添加在线用户失败");
        }

        boolean sent = server.sendPrivateMessage("Root", new ChatMessage(ChatMessage.Type.PRIVATE, "tester", "Root", "hello"));
        if (!sent) {
            throw new AssertionError("私聊目标应当支持昵称匹配");
        }

        System.out.println("ServerPrivateMessageTest passed");
    }

    private static class RecordingHandler extends ClientHandler {
        private final String username;
        private final String nickname;

        private RecordingHandler(Server server, String username, String nickname) {
            super(null, server);
            this.username = username;
            this.nickname = nickname;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getNickname() {
            return nickname;
        }

        @Override
        public void sendMessage(ChatMessage message) {
            System.out.println("message sent to " + username + " -> " + message.getContent());
        }
    }
}
