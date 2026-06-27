package client;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GuiClient extends JFrame {
    /*
     * GUI 聊天窗口，包含接收区域、发送区域、在线用户选择框、昵称修改、文件发送等按钮。
     */
    private JButton sendButton;
    private JButton fileButton;
    private JButton closeButton;
    private JButton nicknameButton;
    private JTextField nicknameField;
    private JTextArea receiveMsg;
    private JTextArea sendMsg;
    private GuiClient frm;
    private Choice clientList;

    public GuiClient(String str) {
        super(str);
        frm = this;

        clientList = new Choice();
        clientList.add("ALL");

        receiveMsg = new JTextArea(15, 40);  // 增加显示行数
        receiveMsg.setEditable(false);       // 设置为不可编辑，防止用户误修改
        receiveMsg.setLineWrap(true);        // 自动换行
        receiveMsg.setWrapStyleWord(true);   // 按单词换行

        sendMsg = new JTextArea(5, 20);

        this.sendMsg.setText("");
        this.receiveMsg.setText("");

        this.sendButton = new JButton("发送");
        this.fileButton = new JButton("发送文件");
        this.closeButton = new JButton("退出");
        this.nicknameButton = new JButton("修改昵称");
        this.nicknameField = new JTextField(10);

        /*
         * p1: 顶部用户选择区
         * p2/p4: 中间消息显示与输入区
         * p3: 底部操作按钮区
         */
        JPanel p = new JPanel();
        JPanel p1 = new JPanel();
        JPanel p2 = new JPanel();
        JPanel p3 = new JPanel();
        JPanel p4 = new JPanel();

        p.setLayout(new BorderLayout());
        p.add(p1, BorderLayout.NORTH);
        p.add(p2, BorderLayout.CENTER);
        p2.add(p4);
        p.add(p3, BorderLayout.SOUTH);

        p1.setLayout(new FlowLayout());
        p1.add(clientList);

        // 创建滚动面板，让消息区域可以滚动查看
        JScrollPane scrollPane = new JScrollPane(receiveMsg);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        p4.setLayout(new GridLayout(2, 1));
        p4.add(scrollPane);  // 添加滚动面板而不是直接添加 receiveMsg
        p4.add(sendMsg);

        p3.setLayout(new FlowLayout());
        p3.add(new JLabel("昵称:"));
        p3.add(this.nicknameField);
        p3.add(this.nicknameButton);
        p3.add(this.sendButton);
        p3.add(this.fileButton);
        p3.add(this.closeButton);

        frm.add(p);
    }

    /**
     * 将窗口居中显示在屏幕上，并设置可见。
     */
    public void showFrame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frm.getSize();
        frm.setLocation((screenSize.width - frameSize.width - 200) / 2,
                (screenSize.height - frameSize.height - 100) / 2);
        frm.pack();
        frm.setVisible(true);
    }

    /**
     * 绑定发送按钮事件。
     */
    public void listenSendButton(final ClientGuiApp b) {
        this.sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b.send();
            }
        });
    }

    /**
     * 绑定文件发送按钮事件。
     */
    public void listenFileButton(final ClientGuiApp b) {
        this.fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b.sendFile();
            }
        });
    }

    /**
     * 绑定退出按钮事件。
     */
    public void listenCloseButton(final ClientGuiApp b) {
        this.closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b.quit();
            }
        });
    }

    /**
     * 绑定昵称修改按钮事件。
     */
    public void listenNickButton(final ClientGuiApp b) {
        this.nicknameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b.sendNickname();
            }
        });
    }

    public String getNicknameText() {
        return nicknameField.getText();
    }

    public String getSendMsg() {
        return sendMsg.getText();
    }

    public void setSendMsgClear() {
        sendMsg.setText("");
    }

    public void setChoiceText(String str) {
        clientList.add(str);
    }

    /**
     * 更新在线用户选择列表。
     */
    public void setChoiceItems(String[] items) {
        clientList.removeAll();
        for (String item : items) {
            clientList.add(item);
        }
        if (clientList.getItemCount() > 0) {
            clientList.select(0);
        }
    }

    public String getChoiceText() {
        return clientList.getSelectedItem();
    }

    /**
     * 将接收消息追加到聊天显示区。
     */
   
    public void setReceiveMsg(String str) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        receiveMsg.append("[" + timestamp + "] " + str + "\r\n");
    }

    public Choice getChoice() {
        return clientList;
    }
}
