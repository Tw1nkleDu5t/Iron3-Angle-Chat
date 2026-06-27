package client;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GuiClientMain extends JFrame {
    /*
     * 登录窗口：用于输入服务器地址、端口、账号和可选昵称。
     * 用户点击“登录”后，程序将尝试连接服务器并进入聊天界面。
     */
    private JTextField address;
    private JTextField port;
    private JTextField I3AAccountField;
    private JTextField nicknameField;
    private JButton loginButton;
    private JButton qutiButton;
    private JFrame clientmainframe;

    public GuiClientMain(String str) {
        super(str);
        clientmainframe = this;
        JLabel JLabel1 = new JLabel("服务器   IP：");
        JLabel JLabel2 = new JLabel("服务器port：");
        JLabel JLabel3 = new JLabel("I3A 账号：");
        JLabel JLabel4 = new JLabel("昵称 (可选)：");
        this.loginButton = new JButton("登录");
        this.qutiButton = new JButton("取消");

        Dimension labelSize = new Dimension(95, 25);
        JLabel1.setPreferredSize(labelSize);
        JLabel1.setHorizontalAlignment(JLabel.RIGHT);
        JLabel2.setPreferredSize(labelSize);
        JLabel2.setHorizontalAlignment(JLabel.RIGHT);
        JLabel3.setPreferredSize(labelSize);
        JLabel3.setHorizontalAlignment(JLabel.RIGHT);
        JLabel4.setPreferredSize(labelSize);
        JLabel4.setHorizontalAlignment(JLabel.RIGHT);

        this.address = new JTextField("127.0.0.1", 18);
        this.port = new JTextField("8000", 18);
        this.I3AAccountField = new JTextField("", 18);
        this.nicknameField = new JTextField("", 18);

        JPanel p1 = new JPanel();
        JPanel p2 = new JPanel();
        JPanel p3 = new JPanel();
        JPanel p4 = new JPanel();
        JPanel p5 = new JPanel();

        p1.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
        p2.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
        p3.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
        p4.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
        p5.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
        p1.add(JLabel1);
        p1.add(this.address);
        p2.add(JLabel2);
        p2.add(this.port);
        p3.add(JLabel3);
        p3.add(this.I3AAccountField);
        p4.add(JLabel4);
        p4.add(this.nicknameField);
        p5.add(this.loginButton);
        p5.add(this.qutiButton);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(5, 1, 0, 4));
        formPanel.add(p1);
        formPanel.add(p2);
        formPanel.add(p3);
        formPanel.add(p4);
        formPanel.add(p5);
        clientmainframe.setLayout(new BorderLayout());
        clientmainframe.add(formPanel, BorderLayout.CENTER);
        clientmainframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * 将登录窗口居中显示在用户屏幕上。
     */
    public void showFrame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = clientmainframe.getSize();
        clientmainframe.setLocation(
                (screenSize.width - frameSize.width - 200) / 2,
                (screenSize.height - frameSize.height - 100) / 2);
        clientmainframe.pack();
        clientmainframe.setVisible(true);
    }

    /**
     * 绑定登录按钮：点击后调用客户端应用的登录逻辑。
     */
    public void listenLoginButton(final ClientGuiApp b) {
        this.loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b.login();
            }
        });
    }

    /**
     * 绑定取消按钮：直接退出程序。
     */
    public void listenQuitButton(final ClientGuiApp b) {
        this.qutiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * 获取用户输入的服务器地址。
     */
    public String getAddress() {
        return address.getText();
    }

    /**
     * 获取用户输入的服务器端口。
     */
    public int getPort() {
        String port1 = port.getText();
        return Integer.parseInt(port1);
    }

    /**
     * 获取用户输入的账号。
     */
    public String getI3AAccount() {
        return I3AAccountField.getText();
    }

    /**
     * 获取用户输入的可选昵称。
     */
    public String getNickname() {
        return nicknameField.getText();
    }
}
