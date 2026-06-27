package server;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class GuiServer extends Frame {
    private JTextArea text = new JTextArea("", 10, 40);
    private JButton startButton = new JButton("开始");
    private JButton closeButton = new JButton("退出");

    private JPanel p1 = new JPanel();
    private JPanel p2 = new JPanel();

    public GuiServer() {
        super("Iron3Angle-Chat 服务器");
        p1.add(startButton);
        p1.add(closeButton);
        p2.add(text);
        this.add(p2, BorderLayout.NORTH);
        this.add(p1, BorderLayout.CENTER);
        this.pack();
        this.setVisible(true);
    }

    public void startButListenter(final Server a) {
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                text.append("正在启动服务器...\r\n");
                a.startServer();
            }
        });
    }

    public void endButtonListenter(final Server a) {
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                a.endServer();
            }
        });
    }

    public void appendLog(String log) {
        text.append(log + "\r\n");
    }

    public void setStartButtonEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
    }

    public void setCloseButtonEnabled(boolean enabled) {
        closeButton.setEnabled(enabled);
    }
}
