package common;

/*
 * Iron3Angle-Chat 专用消息工具类，类名为 MessageUtils。
 *
 * 该工具类负责将 ChatMessage 对象与字符串协议之间互相转换，
 * 保持 Iron3Angle 的消息传输规范一致。
 * 协议格式为：TYPE|sender|target|content
 * 其中 TYPE 表示消息类型，sender 表示发送者，target 表示目标用户或 ALL，content 表示消息内容。
 */
public class MessageUtils {
    /* 协议分隔符，必须与 parse 方法使用的符号一致 */
    private static final String DELIMITER = "|";
    /* 预期分割成的字段数量，防止缺失字段导致解析错误 */
    private static final int EXPECTED_PARTS = 4;

    public static String format(ChatMessage message) {
        /*
         * 将 ChatMessage 对象转换为统一的字符串协议：TYPE|sender|target|content
         * 该协议简洁且易于调试，客户端与服务器必须保持一致。
         * 注：sender、target、content 均会被 sanitize 处理，避免协议分隔符冲突。
         */
        return message.getType() + DELIMITER
                + sanitize(message.getSender()) + DELIMITER
                + sanitize(message.getTarget()) + DELIMITER
                + sanitize(message.getContent());
    }

    public static ChatMessage parse(String text) {
        /*
         * 将传入的协议字符串解析为 ChatMessage 对象，便于业务代码处理。
         * 如果协议不合法，则返回 SYSTEM 系统消息，避免抛异常中断服务器或客户端流程。
         */
        if (text == null) {
            return new ChatMessage(ChatMessage.Type.SYSTEM, "", "", "Invalid message");
        }
        String[] parts = text.split("\\|", EXPECTED_PARTS);
        if (parts.length != EXPECTED_PARTS) {
            return new ChatMessage(ChatMessage.Type.SYSTEM, "", "", "Malformed message: " + text);
        }
        ChatMessage.Type type;
        try {
            type = ChatMessage.Type.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            /*
             * 如果协议中的类型不在枚举范围内，则归类为系统消息，避免抛异常中断流程。
             */
            type = ChatMessage.Type.SYSTEM;
        }
        return new ChatMessage(type, parts[1], parts[2], parts[3]);
    }

    public static String combineStrings(String... pieces) {
        /*
         * 通用字符串组合函数，可用于构造自定义协议消息
         */
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pieces.length; i++) {
            if (i > 0) {
                builder.append(DELIMITER);
            }
            builder.append(sanitize(pieces[i]));
        }
        return builder.toString();
    }

    private static String sanitize(String value) {
        /*
         * 将消息字段中的特殊字符进行简单转义。
         * 目前只处理协议分隔符和换行符，避免字符串内容破坏协议格式。
         */
        return value == null ? "" : value.replace(DELIMITER, "\\").replace("\n", "\\n");
    }
}
