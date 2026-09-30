// Student Name: Sreeja Gangavarapu
// Student Number: 999901976

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;

public class Minesweeper extends JFrame implements ActionListener, MouseListener {

    // Difficulty presets
    private static final int EASY_SIZE = 6, EASY_MINES = 11;
    private static final int MEDIUM_SIZE = 12, MEDIUM_MINES = 36;
    private static final int HARD_SIZE = 21, HARD_MINES = 92;

    String diffic;
    int size;
    int numMines;

    boolean[][] mines;
    boolean[][] clicked;
    boolean[][] flagged;
    int[][] cells;

    JButton[][] buttons;
    JPanel panel;
    JFrame window;

    int totFlagged = 0;
    javax.swing.Timer timer; // Swing timer

    public static void main(String[] args) {
        new Minesweeper(11, "EASY");
    }

    public Minesweeper(int noOfMines, String diffic) {
        this.diffic = diffic;
        this.numMines = noOfMines;

        // Set size & mines from difficulty
        if ("EASY".equals(diffic)) {
            size = EASY_SIZE; numMines = EASY_MINES;
        } else if ("MEDIUM".equals(diffic)) {
            size = MEDIUM_SIZE; numMines = MEDIUM_MINES;
        } else if ("HARD".equals(diffic)) {
            size = HARD_SIZE; numMines = HARD_MINES;
        } else {
            size = EASY_SIZE; numMines = EASY_MINES;
        }

        // Allocate arrays AFTER size is known
        mines   = new boolean[size][size];
        clicked = new boolean[size][size];
        flagged = new boolean[size][size];
        cells   = new int[size][size];
        buttons = new JButton[size][size];

        // Window
        window = new JFrame("MINESWEEPER");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(50 * size, 50 * size);
        window.setLocationRelativeTo(null);

        // Menu
        JMenuBar bar = new JMenuBar();
        JMenu diff = new JMenu("Difficulty");
        JMenu curDifficulty = new JMenu("current: " + diffic);
        JMenu restart = new JMenu("........RESET GAME........");
        JLabel timerLabel = new JLabel("Time left: 0 seconds");

        JMenuItem easy = new JMenuItem("EASY");    easy.addActionListener(this);
        JMenuItem med  = new JMenuItem("MEDIUM");  med.addActionListener(this);
        JMenuItem hard = new JMenuItem("HARD");    hard.addActionListener(this);
        JMenuItem cust = new JMenuItem("RUSSIAN ROULETTE"); cust.addActionListener(this);
        JMenuItem confirm = new JMenuItem("CONFIRM"); confirm.addActionListener(this);
        JMenuItem cancel  = new JMenuItem("CANCEL");

        diff.add(easy); diff.add(med); diff.add(hard); diff.add(cust);
        restart.add(confirm); restart.add(cancel);

        bar.add(diff);
        bar.add(curDifficulty);
        bar.add(Box.createHorizontalGlue());
        bar.add(timerLabel);
        bar.add(restart);

        // Board
        panel = new JPanel(new GridLayout(size, size));
        initialise();
        populateMines();
        populateCells();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                panel.add(buttons[i][j]);
            }
        }

        window.setJMenuBar(bar);
        window.add(panel, BorderLayout.CENTER);

        // Timer duration
        int duration;
        if ("EASY".equals(diffic))        duration = 11 * 60;
        else if ("MEDIUM".equals(diffic)) duration = 36 * 60;
        else                               duration = 92 * 60;  // HARD / RUSSIAN ROULETTE

        timer = new javax.swing.Timer(1000, new ActionListener() {
            int timeLeft = duration;
            @Override public void actionPerformed(ActionEvent e) {
                timeLeft--;
                timerLabel.setText("Time left: " + timeLeft + " seconds");
                if (timeLeft <= 0) {
                    ((javax.swing.Timer)e.getSource()).stop();
                    explode();
                }
            }
        });
        timer.start();

        window.setVisible(true);
    }

    // MouseListener
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            JButton b = (JButton) e.getSource();
            String index = (String) b.getClientProperty("index");

            int rowStart = index.indexOf("r:") + 2;
            int rowEnd = index.indexOf(" ", rowStart);
            int row = Integer.parseInt(index.substring(rowStart, rowEnd));

            int colStart = index.indexOf("c:") + 2;
            int col = Integer.parseInt(index.substring(colStart));

            if (b.isEnabled() && !clicked[row][col]) b.setIcon(new ImageIcon("flag2.gif"));

            if (flagged[row][col] && !clicked[row][col]) {
                flagged[row][col] = false; totFlagged--; b.setIcon(null); check(); return;
            }
            if (!flagged[row][col] && !clicked[row][col]) {
                flagged[row][col] = true; totFlagged++;
            }
            check();
        }
    }

    // ActionListener
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if ("HARD".equals(cmd))             { refresh(HARD_MINES, "HARD"); return; }
        if ("EASY".equals(cmd))             { refresh(EASY_MINES, "EASY"); return; }
        if ("MEDIUM".equals(cmd))           { refresh(MEDIUM_MINES, "MEDIUM"); return; }
        if ("CONFIRM".equals(cmd))          { refresh(); return; }
        if ("RUSSIAN ROULETTE".equals(cmd)) { refresh(HARD_MINES, "RUSSIAN ROULETTE"); return; }

        // Left-click on a cell
        check();
        JButton b = (JButton) e.getSource();
        String index = (String) b.getClientProperty("index");

        int rowStart = index.indexOf("r:") + 2;
        int rowEnd = index.indexOf(" ", rowStart);
        int row = Integer.parseInt(index.substring(rowStart, rowEnd));

        int colStart = index.indexOf("c:") + 2;
        int col = Integer.parseInt(index.substring(colStart));

        if (flagged[row][col]) {
            flagged[row][col] = false; totFlagged--;
            check(); buttons[row][col].setText(""); buttons[row][col].setIcon(null);
            return;
        }

        if (mines[row][col]) {
            explode();
        } else if (cells[row][col] > 0) {
            b.setText("" + cells[row][col]);
            b.setForeground(Color.BLACK);
            clicked[row][col] = true;
        } else {
            zeroClear(row, col);
        }
        check();
    }

    // Flood reveal
    public void zeroClear(int i, int j) {
        clicked[i][j] = true;
        buttons[i][j].setBackground(Color.BLACK);
        buttons[i][j].setEnabled(false);

        boolean left  = j - 1 >= 0;
        boolean right = j + 1 < size;
        boolean up    = i - 1 >= 0;
        boolean down  = i + 1 < size;

        if (left) {
            if (cells[i][j-1] == 0 && !clicked[i][j-1] && !flagged[i][j-1]) {
                buttons[i][j-1].setText(""); buttons[i][j-1].setBackground(Color.BLACK);
                buttons[i][j-1].setForeground(Color.BLACK);
                clicked[i][j-1] = true; zeroClear(i, j-1);
            } else if (cells[i][j-1] == 0 && !flagged[i][j-1]) {
                buttons[i][j-1].setText("");
            } else if (!flagged[i][j-1]) {
                buttons[i][j-1].setText("" + cells[i][j-1]); clicked[i][j-1] = true;
            }
        }
        if (right) {
            if (cells[i][j+1] == 0 && !clicked[i][j+1] && !flagged[i][j+1]) {
                buttons[i][j+1].setText(""); buttons[i][j+1].setBackground(Color.BLACK);
                buttons[i][j+1].setForeground(Color.BLACK);
                clicked[i][j+1] = true; zeroClear(i, j+1);
            } else if (cells[i][j+1] == 0 && !flagged[i][j+1]) {
                buttons[i][j+1].setText("");
            } else if (!flagged[i][j+1]) {
                buttons[i][j+1].setText("" + cells[i][j+1]); clicked[i][j+1] = true;
            }
        }
        if (up) {
            if (cells[i-1][j] == 0 && !clicked[i-1][j] && !flagged[i-1][j]) {
                buttons[i-1][j].setText(""); buttons[i-1][j].setBackground(Color.BLACK);
                buttons[i-1][j].setForeground(Color.BLACK);
                clicked[i-1][j] = true; zeroClear(i-1, j);
            } else if (cells[i-1][j] == 0 && !flagged[i-1][j]) {
                buttons[i-1][j].setText("");
            } else if (!flagged[i-1][j]) {
                buttons[i-1][j].setText("" + cells[i-1][j]); clicked[i-1][j] = true;
            }
        }
        if (down) {
            if (cells[i+1][j] == 0 && !clicked[i+1][j] && !flagged[i+1][j]) {
                buttons[i+1][j].setText(""); buttons[i+1][j].setBackground(Color.BLACK);
                buttons[i+1][j].setForeground(Color.BLACK);
                clicked[i+1][j] = true; zeroClear(i+1, j);
            } else if (cells[i+1][j] == 0 && !flagged[i+1][j]) {
                buttons[i+1][j].setText("");
            } else if (!flagged[i+1][j]) {
                buttons[i+1][j].setText("" + cells[i+1][j]); clicked[i+1][j] = true;
            }
        }
    }

    public void initialise() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                mines[i][j] = false; clicked[i][j] = false; flagged[i][j] = false; cells[i][j] = 0;
                buttons[i][j] = new JButton();
                buttons[i][j].addActionListener(this);
                buttons[i][j].addMouseListener(this);
                buttons[i][j].putClientProperty("index", "r:" + i + " c:" + j);
            }
        }
    }

    public void populateMines() {
        int needed = numMines;
        while (needed > 0) {
            int x = (int) Math.floor(Math.random() * size);
            int y = (int) Math.floor(Math.random() * size);
            if (!mines[x][y]) { mines[x][y] = true; needed--; }
        }
    }

    public void populateCells() {
        int count = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                boolean left  = j - 1 >= 0;
                boolean right = j + 1 < size;
                boolean up    = i - 1 >= 0;
                boolean down  = i + 1 < size;

                if (left) {
                    if (mines[i][j-1]) count++;
                    if (up   && mines[i-1][j-1]) count++;
                    if (down && mines[i+1][j-1]) count++;
                }
                if (right) {
                    if (mines[i][j+1]) count++;
                    if (up   && mines[i-1][j+1]) count++;
                    if (down && mines[i+1][j+1]) count++;
                }
                if (up   && mines[i-1][j]) count++;
                if (down && mines[i+1][j]) count++;

                cells[i][j] = count; count = 0;
            }
        }
    }

    public void check() {
        int count = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (clicked[i][j] || flagged[i][j]) {
                    count++;
                    if (totFlagged == numMines && count == (size * size)) {
                        if ("RUSSIAN ROULETTE".equals(diffic)) {
                            JOptionPane.showMessageDialog(null,
                                    new ImageIcon("hell.gif"),
                                    "AW HELL NAW!!!?  WELL DONE!",
                                    JOptionPane.INFORMATION_MESSAGE,
                                    new ImageIcon("WIN77.gif"));
                            refresh(); break;
                        }
                        JOptionPane.showMessageDialog(null,
                                new ImageIcon("Clap.gif"),
                                "CONGRATULATIONS!",
                                JOptionPane.INFORMATION_MESSAGE,
                                new ImageIcon("WIN.gif"));
                        if ("EASY".equals(diffic))   { refresh(MEDIUM_MINES, "MEDIUM"); break; }
                        if ("MEDIUM".equals(diffic)) { refresh(HARD_MINES, "HARD"); break; }
                        if ("HARD".equals(diffic))   { refresh(HARD_MINES, "RUSSIAN ROULETTE"); break; }
                    }
                } else {
                    break;
                }
            }
        }
    }

    private void playExplosionSound() {
        try {
            AudioInputStream audioInputStream =
                    AudioSystem.getAudioInputStream(new java.io.File("explosion.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (UnsupportedAudioFileException | LineUnavailableException |
                 java.io.IOException | IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void explode() {
        playExplosionSound();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (mines[i][j]) {
                    Icon imgIcon = new ImageIcon("Ex2.gif"); // matches your filename
                    buttons[i][j].setIcon(imgIcon);
                }
            }
        }
        JOptionPane.showMessageDialog(null,
                new ImageIcon("Excloud.gif"), "",
                JOptionPane.INFORMATION_MESSAGE, new ImageIcon("unlucky.gif"));
        refresh();
    }

    public void refresh() {
        window.setVisible(false);
        new Minesweeper(numMines, diffic);
    }

    public void refresh(int numMin, String difficul) {
        window.setVisible(false);
        new Minesweeper(numMin, difficul);
    }
}
