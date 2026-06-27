package common;

/*
 * 聊天消息对象，用于客户端与服务器之间的协议传递。
 *
 * 这个类体现了 Iron3Angle 项目中“消息协议统一”的思想，
 * 继承了原始设计中 ChatMessage 的核心结构：类型(type)、发送者(sender)、目标(target)、内容(content)。
 * 通过统一协议传递，保证服务器和客户端之间兼容性与可维护性。
 */
public class ChatMessage {
    /*
     * 消息类型枚举，用于定义客户端与服务器之间可以交换的消息类别。
     * 这种方式让协议清晰且易于扩展，新增类型只需在这里添加即可。
     */
    public enum Type {
        /* 登录消息，用于客户端首次连接后发送 I3A 账号 和必要信息 */
        LOGIN,
        /* 登出消息，用于客户端主动退出时通知服务器 */
        LOGOUT,
        /* 群聊消息，发送给所有在线 I3A 账号 */
        MESSAGE,
        /* 私聊消息，发送给指定目标 I3A 账号 */
        PRIVATE,
        /* 文件传输消息，用于发送文件内容和文件名 */
        FILE,
        /* 昵称更新消息，用于客户端设置或修改显示昵称 */
        NICKNAME,
        /* 用户列表消息，由服务器发送给客户端，告知当前在线 I3A 账号及昵称 */
        USERS,
        /* 系统消息，用于错误、提示、状态更新等 */
        SYSTEM
    }

    private final Type type;
    private final String sender;
    private final String target;
    private final String content;

    public ChatMessage(Type type, String sender, String target, String content) {
        this.type = type;
        this.sender = sender == null ? "" : sender;
        this.target = target == null ? "" : target;
        this.content = content == null ? "" : content;
    }

    /**
     * 获取消息类型。
     * 例如 LOGIN、MESSAGE、FILE 等。
     */
    public Type getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getTarget() {
        return target;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        /*
         * 调试友好的输出：将消息对象转换成协议字符串。
         * 这对于日志记录或控制台打印非常有用。
         */
        return MessageUtils.format(this);
    }
}
