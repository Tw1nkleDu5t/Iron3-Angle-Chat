package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryLogger {
    /*
     * 历史记录目录和文件名。
     * 讲解时可以说明：所有聊天记录与收到的文件都保存在项目根目录下，便于演示文件结构。
     */
    private static final String HISTORY_DIR = "历史聊天记录";
    private static final String FILE_NAME = "chat_history.txt";
    private static final String RECEIVED_DIR = "历史聊天记录/received_files";

    /**
     * 将一行历史记录追加到日志文件。
     *
     * @param line 要保存的日志内容，通常包括时间戳和消息文本。
     */
    public static synchronized void log(String line) {
        try {
            File dir = new File(HISTORY_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, FILE_NAME);
            try (BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                writer.write(timestamp() + " " + line);
                writer.newLine();
            }
        } catch (IOException ignored) {
            /* 忽略写日志失败，避免影响聊天功能 */
        }

    }

    /**
     * 将接收到的文件二进制保存到本地目录。
     *
     * @param fileName 原始文件名
     * @param content  解码后的二进制内容
     * @return 保存后的绝对路径，或保存失败时返回 null
     */
    public static synchronized String saveReceivedFile(String fileName, byte[] content) {
        try {
            File dir = new File(RECEIVED_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File target = new File(dir, fileName);
            try (FileOutputStream stream = new FileOutputStream(target)) {
                stream.write(content);
            }
            return target.getAbsolutePath();
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * 生成当前时间戳，用于日志记录的前缀。
     */
    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
