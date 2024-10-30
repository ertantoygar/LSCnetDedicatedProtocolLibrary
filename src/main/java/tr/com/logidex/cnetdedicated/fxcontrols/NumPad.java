package tr.com.logidex.cnetdedicated.fxcontrols;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * The NumPad class represents a custom numeric keypad interface within a JFrame.
 * This class provides singleton access and allows you to set minimum and maximum values for input.
 * It extends javax.swing.JFrame and integrates with Java's AWT Robot class for automation tasks.
 */
public class NumPad extends JFrame {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static NumPad instance = null;
    private final JPanel contentPane;
    JLabel lblNewLabel = new JLabel("");
    private Point initialClick;
    private Robot robot;


    /**
     * Create the frame.
     */
    private NumPad() {
        try {
            robot = new Robot();
        } catch (AWTException e1) {
            e1.printStackTrace();
        }
        setAlwaysOnTop(true);
        setFocusable(false);
        setFocusableWindowState(false);
        setUndecorated(true);
        setType(Type.UTILITY);
        setTitle("NumPad");
        setLocationRelativeTo(getParent());
        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(300, 100, 286, 289);
        contentPane = new JPanel();
        contentPane.setOpaque(false);
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
        JPanel panel = new JPanel();
        panel.setBackground(new Color(33, 33, 33));
        panel.setBorder(new LineBorder(new Color(11, 11, 11), 1, true));
        panel.setPreferredSize(new Dimension(300, 500));
        contentPane.add(panel, BorderLayout.CENTER);
        JButton btn7 = new JButton("7");
        btn7.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_7);
            }
        });
        btn7.setForeground(new Color(255, 255, 255));
        btn7.setBackground(new Color(45, 55, 65));
        btn7.setPreferredSize(new Dimension(25, 25));
        btn7.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn8 = new JButton("8");
        btn8.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_8);
            }
        });
        btn8.setForeground(new Color(255, 255, 255));
        btn8.setBackground(new Color(45, 55, 65));
        btn8.setPreferredSize(new Dimension(25, 25));
        btn8.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn9 = new JButton("9");
        btn9.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_9);
            }
        });
        btn9.setForeground(new Color(255, 255, 255));
        btn9.setBackground(new Color(45, 55, 65));
        btn9.setPreferredSize(new Dimension(25, 25));
        btn9.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn4 = new JButton("4");
        btn4.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_4);
            }
        });
        btn4.setForeground(new Color(255, 255, 255));
        btn4.setBackground(new Color(45, 55, 65));
        btn4.setPreferredSize(new Dimension(25, 25));
        btn4.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn5 = new JButton("5");
        btn5.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_5);
            }
        });
        btn5.setForeground(new Color(255, 255, 255));
        btn5.setBackground(new Color(45, 55, 65));
        btn5.setPreferredSize(new Dimension(25, 25));
        btn5.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn6 = new JButton("6");
        btn6.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_6);
            }
        });
        btn6.setForeground(new Color(255, 255, 255));
        btn6.setBackground(new Color(45, 55, 65));
        btn6.setPreferredSize(new Dimension(25, 25));
        btn6.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn1 = new JButton("1");
        btn1.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_1);
            }
        });
        btn1.setForeground(new Color(255, 255, 255));
        btn1.setBackground(new Color(45, 55, 65));
        btn1.setPreferredSize(new Dimension(25, 25));
        btn1.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn2 = new JButton("2");
        btn2.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_2);
            }
        });
        btn2.setForeground(new Color(255, 255, 255));
        btn2.setBackground(new Color(45, 55, 65));
        btn2.setPreferredSize(new Dimension(25, 25));
        btn2.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn3 = new JButton("3");
        btn3.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_3);
            }
        });
        btn3.setForeground(new Color(255, 255, 255));
        btn3.setBackground(new Color(45, 55, 65));
        btn3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        btn3.setPreferredSize(new Dimension(25, 25));
        btn3.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btn0 = new JButton("0");
        btn0.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_0);
            }
        });
        btn0.setForeground(new Color(255, 255, 255));
        btn0.setBackground(new Color(45, 55, 65));
        btn0.setPreferredSize(new Dimension(25, 25));
        btn0.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btnNeg = new JButton("-");
        btnNeg.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_MINUS);
            }
        });
        btnNeg.setForeground(new Color(255, 255, 255));
        btnNeg.setBackground(new Color(45, 55, 65));
        btnNeg.setPreferredSize(new Dimension(25, 25));
        btnNeg.setFont(new Font("Tahoma", Font.BOLD, 25));
        JButton btnDot = new JButton(".");
        btnDot.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(0x2e);
            }
        });
        btnDot.setForeground(new Color(255, 255, 255));
        btnDot.setBackground(new Color(45, 55, 65));
        btnDot.setPreferredSize(new Dimension(25, 25));
        btnDot.setFont(new Font("Tahoma", Font.BOLD, 25));
        btnDot.setVisible(true);
        JButton btnBack = new JButton("Back");
        btnBack.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_BACK_SPACE);
            }
        });
        btnBack.setForeground(new Color(255, 255, 255));
        btnBack.setBackground(new Color(45, 55, 65));
        btnBack.setPreferredSize(new Dimension(25, 25));
        btnBack.setFont(new Font("Tahoma", Font.BOLD, 14));
        JButton btnEnter = new JButton("Enter");
        btnEnter.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_ENTER);
                NumPad.this.setVisible(false);
            }
        });
        btnEnter.setForeground(new Color(255, 255, 255));
        btnEnter.setBackground(new Color(45, 55, 65));
        btnEnter.setPreferredSize(new Dimension(25, 25));
        btnEnter.setFont(new Font("Tahoma", Font.BOLD, 14));
        JButton btnPrev = new JButton("<");
        btnPrev.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_LEFT);
            }
        });
        btnPrev.setForeground(new Color(255, 255, 255));
        btnPrev.setBackground(new Color(45, 55, 65));
        btnPrev.setPreferredSize(new Dimension(50, 25));
        btnPrev.setFont(new Font("Tahoma", Font.BOLD, 14));
        JButton btnNext = new JButton(">");
        btnNext.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                robot.keyPress(KeyEvent.VK_RIGHT);
            }
        });
        btnNext.setForeground(new Color(255, 255, 255));
        btnNext.setBackground(new Color(45, 55, 65));
        btnNext.setPreferredSize(new Dimension(50, 25));
        btnNext.setFont(new Font("Tahoma", Font.BOLD, 14));
        JButton btnCancel = new JButton("ESC");
        btnCancel.setVisible(false);
        btnCancel.setForeground(new Color(255, 255, 255));
        btnCancel.setBackground(new Color(45, 55, 65));
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NumPad.this.setVisible(false);
            }
        });
        btnCancel.setPreferredSize(new Dimension(25, 25));
        btnCancel.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblNewLabel.setForeground(new Color(102, 153, 255));
        lblNewLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(gl_panel.createSequentialGroup().addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addContainerGap())
                .addGroup(gl_panel.createSequentialGroup().addContainerGap().addGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
                                        .addGroup(gl_panel.createSequentialGroup().addComponent(btn1, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addGap(6)
                                                .addComponent(btn2, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addGap(6).addComponent(btn3, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(gl_panel.createSequentialGroup().addComponent(btn4, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addGap(6)
                                                .addComponent(btn5, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addGap(6).addComponent(btn6, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)))
                                .addGroup(gl_panel.createSequentialGroup().addComponent(btn0, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(btnNeg, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(btnDot, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE))
                                .addGroup(gl_panel.createSequentialGroup().addComponent(btn7, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(btn8, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(btn9, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE))
                                .addGroup(gl_panel.createSequentialGroup().addComponent(btnPrev, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE).addGap(6).addComponent(btnNext, GroupLayout.PREFERRED_SIZE, 54,
                                        GroupLayout.PREFERRED_SIZE)))
                        .addGap(18)
                        .addGroup(gl_panel.createParallelGroup(Alignment.TRAILING).addComponent(btnCancel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnEnter, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnBack, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                        .addGap(17)));
        gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.TRAILING).addGroup(gl_panel.createSequentialGroup().addGap(15).addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
                        .addGroup(gl_panel.createParallelGroup(Alignment.LEADING).addComponent(btn7, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE).addComponent(btn8, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn9, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE))
                        .addComponent(btnBack, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)).addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
                        .addGroup(gl_panel.createSequentialGroup()
                                .addGroup(gl_panel.createParallelGroup(Alignment.LEADING).addComponent(btn4, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn5, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE).addComponent(btn6, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(gl_panel.createParallelGroup(Alignment.LEADING).addComponent(btn1, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn2, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE).addComponent(btn3, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(gl_panel.createParallelGroup(Alignment.LEADING).addComponent(btn0, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(gl_panel.createParallelGroup(Alignment.BASELINE).addComponent(btnNeg, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE).addComponent(btnDot,
                                                GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE))))
                        .addComponent(btnEnter, GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(gl_panel.createParallelGroup(Alignment.LEADING).addComponent(btnPrev, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE).addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnNext, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE).addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(ComponentPlacement.RELATED).addComponent(lblNewLabel).addGap(9)));
        panel.setLayout(gl_panel);
        NumPad.this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }
        });
        NumPad.this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // get location of Window
                int thisX = NumPad.this.getLocation().x;
                int thisY = NumPad.this.getLocation().y;
                // Determine how much the mouse moved since the initial click
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                // Move window to this position
                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                NumPad.this.setLocation(X, Y);
            }
        });
    }


    public static NumPad getInstance() {
        if (instance == null) {
            instance = new NumPad();
        }
        return instance;
    }


    public void setMinMax(float minValue, float maxValue) {
        lblNewLabel.setText("Min:" + minValue + "  Max:" + maxValue);
    }
}
