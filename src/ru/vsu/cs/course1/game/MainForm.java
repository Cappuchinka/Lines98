package ru.vsu.cs.course1.game;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ru.vsu.cs.util.DrawUtils;
import ru.vsu.cs.util.JTableUtils;
import ru.vsu.cs.util.SwingUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

public class MainForm extends JFrame {
    private JPanel panelMain;
    private JTable tableGameField;
    private JLabel labelStatus;
    private JButton buttonNewGame;
    private JPanel panelTop;
    private JLabel labelScore;
    private JPanel panelScore;
    private JLabel labelIcon;
    private JScrollPane scrollPanel;

    private static final int DEFAULT_COL_COUNT = 9;
    private static final int DEFAULT_ROW_COUNT = 9;

    private static final int DEFAULT_GAP = 8;
    private static final int DEFAULT_CELL_SIZE = 40;

    private static final Color[] COLORS = {
            Color.RED,
            new Color(128, 0, 0), //DARK_RED
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA,
            Color.GREEN,
            Color.YELLOW,
    };

    private static final Game.BallCell NULL_CELL = new Game.BallCell(Game.CellState.NO_BALL);

    private LinesGameParams params = new LinesGameParams(DEFAULT_ROW_COUNT, DEFAULT_COL_COUNT);
    private Game game = new Game();

    private ParamsDialog dialogParams;

    public MainForm() {
        this.setTitle("Lines98");
        this.setContentPane(panelMain);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();

        setJMenuBar(createMenuBar());
        this.pack();

        scrollPanel.setBackground(new Color(40, 40, 40));
        scrollPanel.setForeground(new Color(40, 40, 40));
        scrollPanel.setOpaque(true);
        scrollPanel.getViewport().setBackground(new Color(40, 40, 40));

        SwingUtils.setShowMessageDefaultErrorHandler();

        tableGameField.setRowHeight(DEFAULT_CELL_SIZE);
        JTableUtils.initJTableForArray(tableGameField, DEFAULT_CELL_SIZE, false, false, false, false);
        tableGameField.setIntercellSpacing(new Dimension(0, 0));
        tableGameField.setEnabled(false);

        tableGameField.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            final class DrawComponent extends Component {
                private int row = 0, column = 0;

                @Override
                public void paint(Graphics gr) {
                    Graphics2D g2d = (Graphics2D) gr;
                    int width = getWidth() - 2;
                    int height = getHeight() - 2;
                    paintCell(row, column, g2d, width, height);
                }
            }

            DrawComponent comp = new DrawComponent();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                comp.row = row;
                comp.column = column;
                return comp;
            }
        });

        newGame();

        updateWindowSize();
        updateView();
        buttonNewGame.addActionListener(e -> {
            newGame();
            labelScore.setText("" + 0);
        });

        dialogParams = new ParamsDialog(params, tableGameField, e -> newGame());

        tableGameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = tableGameField.rowAtPoint(e.getPoint());
                int col = tableGameField.columnAtPoint(e.getPoint());
                if (SwingUtilities.isLeftMouseButton(e)) {
                    game.leftMouseClick(row, col);
                    updateView();
                }
            }
        });
    }

    private JMenuItem createMenuItem(String text, String shortcut, Character mnemonic, ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(listener);
        if (shortcut != null) {
            menuItem.setAccelerator(KeyStroke.getKeyStroke(shortcut.replace('+', ' ')));
        }
        if (mnemonic != null) {
            menuItem.setMnemonic(mnemonic);
        }
        return menuItem;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBarMain = new JMenuBar();

        JMenu menuGame = new JMenu("Игра");
        menuBarMain.add(menuGame);
        menuGame.add(createMenuItem("Новая", "ctrl+N", null, e -> {
            newGame();
        }));
        menuGame.add(createMenuItem("Параметры", "ctrl+P", null, e -> {
            dialogParams.updateView();
            dialogParams.setVisible(true);
        }));
        menuGame.addSeparator();
        menuGame.add(createMenuItem("Выход", "ctrl+X", null, e -> {
            System.exit(0);
        }));

        JMenu menuView = new JMenu("Вид");
        menuBarMain.add(menuView);
        menuView.add(createMenuItem("Подогнать размер окна", null, null, e -> {
            updateWindowSize();
        }));
        menuView.addSeparator();
        SwingUtils.initLookAndFeelMenu(menuView);

        JMenu menuHelp = new JMenu("Справка");
        menuBarMain.add(menuHelp);
        menuHelp.add(createMenuItem("Правила", "ctrl+R", null, e -> {
            SwingUtils.showInfoMessageBox("Задача игрока заключается в том, чтобы собрать линию из шариков одинакового цвета." +
                    "\n" +
                    "Такая линия может иметь горизонтальное, вертикальное либо диагональное направление." +
                    "\n" +
                    "Для того, чтобы шарики лопнули, а игрок заработал свои очки, нужно, " +
                    "\n" +
                    "чтобы в линию были собраны не менее пяти шариков одинакового цвета. " +
                    "\n" +
                    "Управляя мышкой, игрок перемещает нужный шар на любое расстояние. " +
                    "\n" +
                    "Однако если «проход» перекрыт, то шарик не может быть перемещен. " +
                    "\n" +
                    "По диагонали шарик не двигается. " +
                    "\n" +
                    "После каждого хода, который не привел к уничтожению шаров и получению очков, " +
                    "\n" +
                    "хаотично высыпаются три шара любого цвета.\n" +
                    "\n" +
                    "Игровое поле представляет собой серый лист в клетку на сером фоне." +
                    "\n" +
                    "Количество клеток по горизонтали и вертикали равно девяти. " +
                    "\n" +
                    "Когда пользователь начинает играть, на поле его ждут всего пять шаров. " +
                    "\n" +
                    "Постепенно поле заполняется шарами, и уничтожить все невозможно, " +
                    "\n" +
                    "да и не это является целью игры. " +
                    "\n" +
                    "Цель игры Lines98 состоит в том, чтобы набрать как можно больше очков, " +
                    "\n" +
                    "а для этого нужны шары на поле.", "Правила");
        }));
        menuHelp.add(createMenuItem("О программе", "ctrl+A", null, e -> {
            SwingUtils.showInfoMessageBox(
                    "Lines98" +
                            "\n\nАвтор: Волченко П.В. (Kappućinka)" +
                            "\nE-mail: volchenko.coffee@gmail.com" +
                            "\nInstagram: medajibka",
                    "О игре"
            );
        }));

        return menuBarMain;
    }

    private void updateWindowSize() {
        int menuSize = this.getJMenuBar() != null ? this.getJMenuBar().getHeight() : 0;
        SwingUtils.setFixedSize(
                this,
                tableGameField.getWidth() + 2 * DEFAULT_GAP + 23,
                tableGameField.getHeight() + panelMain.getY() + labelStatus.getHeight() +
                        menuSize + 1 * DEFAULT_GAP + 2 * DEFAULT_GAP + 77
        );
        this.setMaximumSize(null);
        this.setMinimumSize(null);
    }

    private void updateView() {
        tableGameField.repaint();
        if (game.getState() == Game.GameState.PLAYING) {
            labelStatus.setForeground(Color.GREEN);
            labelStatus.setText("PLAYING");
        } else {
            if (game.getState() == Game.GameState.GAME_OVER) {
                labelStatus.setForeground(Color.RED);
                labelStatus.setText("GAME OVER :-(");
            }
        }
        labelScore.setText("" + game.getScore());
    }

    private Font font = null;

    private Font getFont(int size) {
        if (font == null || font.getSize() != size) {
            font = new Font("Supercell-Magic", Font.BOLD, size);
        }
        return font;
    }

    private void paintCell(int row, int column, Graphics2D g2d, int cellWidth, int cellHeight) {
        Game.BallCell cellValue = game.getCell(row, column);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (cellValue == null) {
            cellValue = NULL_CELL;
        }

        Color colorOfBall;
        int index = 0;
        if (cellValue.getColor() == Game.Colors.DARK_RED) {
            index = 1;
        } else if (cellValue.getColor() == Game.Colors.BLUE) {
            index = 2;
        } else if (cellValue.getColor() == Game.Colors.CYAN) {
            index = 3;
        } else if (cellValue.getColor() == Game.Colors.MAGENTA) {
            index = 4;
        } else if (cellValue.getColor() == Game.Colors.GREEN) {
            index = 5;
        } else if (cellValue.getColor() == Game.Colors.YELLOW) {
            index = 6;
        }
        colorOfBall = COLORS[index];

        int size = Math.min(cellWidth, cellHeight);

        Color backColor = Color.GRAY;
        g2d.setColor(backColor);
        g2d.fillRect(0, 0, size, size);

        Color color = DrawUtils.getContrastColor(backColor);
        char ch = ' ';
        if (game.getState() == Game.GameState.GAME_OVER) {
            g2d.setColor(colorOfBall);
            g2d.fillOval(1, 1, size - 2, size - 2);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(1, 1, size - 2, size - 2);
        } else {
            if (cellValue.getState() == Game.CellState.BALL_STANDS) {
                g2d.setColor(colorOfBall);
                g2d.fillOval(7, 7, size - 15, size - 15);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(7, 7, size - 15, size - 15);
            } else if (cellValue.getState() == Game.CellState.BALL_WILL_APPEAR) {
                g2d.setColor(colorOfBall);
                g2d.fillOval(DEFAULT_CELL_SIZE / 4 + 4, DEFAULT_CELL_SIZE / 4 + 4, size - 30, size - 30);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(DEFAULT_CELL_SIZE / 4 + 4, DEFAULT_CELL_SIZE / 4 + 4, size - 30, size - 30);
            } else if (cellValue.getState() == Game.CellState.BALL_JUMPING) {
                g2d.setColor(colorOfBall);
                g2d.fillOval(1, 1, size - 2, size - 2);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(1, 1, size - 2, size - 2);
            }
        }
        g2d.setColor(color);
        int bound = (int) Math.round(size * 0.1);
        Font font = getFont(size - 2 * bound);
        DrawUtils.drawStringInCenter(g2d, font, "" + ch, 0, 0, cellWidth, (int) Math.round(cellHeight * 0.95));
    }

    private void newGame() {
        game.newGame(params.getRowCount(), params.getColCount());
        JTableUtils.resizeJTable(tableGameField,
                game.getRowCount(), game.getColCount(),
                tableGameField.getRowHeight(), tableGameField.getRowHeight()
        );
        labelScore.setText("" + 0);
        updateView();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panelMain = new JPanel();
        panelMain.setLayout(new GridLayoutManager(3, 3, new Insets(10, 10, 10, 10), -1, 10));
        panelMain.setBackground(new Color(-14145496));
        panelMain.setForeground(new Color(-16777216));
        scrollPanel = new JScrollPane();
        scrollPanel.setBackground(new Color(-13158601));
        scrollPanel.setForeground(new Color(-13158601));
        panelMain.add(scrollPanel, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableGameField = new JTable();
        tableGameField.setGridColor(new Color(-16777216));
        scrollPanel.setViewportView(tableGameField);
        panelTop = new JPanel();
        panelTop.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelTop.setBackground(new Color(-14145496));
        panelMain.add(panelTop, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panelScore = new JPanel();
        panelScore.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panelScore.setBackground(new Color(-14803426));
        panelScore.setEnabled(false);
        panelTop.add(panelScore, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, 30), null, 0, false));
        labelScore = new JLabel();
        labelScore.setBackground(new Color(-16711936));
        Font labelScoreFont = this.$$$getFont$$$("AngryBirds", -1, 20, labelScore.getFont());
        if (labelScoreFont != null) labelScore.setFont(labelScoreFont);
        labelScore.setForeground(new Color(-16711936));
        labelScore.setHorizontalTextPosition(4);
        labelScore.setText("0");
        panelScore.add(labelScore, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        labelIcon = new JLabel();
        labelIcon.setIcon(new ImageIcon(getClass().getResource("/image120x32.jpg")));
        labelIcon.setText("");
        panelTop.add(labelIcon, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelStatus = new JLabel();
        Font labelStatusFont = this.$$$getFont$$$("AngryBirds", Font.PLAIN, 18, labelStatus.getFont());
        if (labelStatusFont != null) labelStatus.setFont(labelStatusFont);
        labelStatus.setText("Label");
        panelMain.add(labelStatus, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonNewGame = new JButton();
        buttonNewGame.setBackground(new Color(-12500671));
        Font buttonNewGameFont = this.$$$getFont$$$("AngryBirds", Font.PLAIN, 18, buttonNewGame.getFont());
        if (buttonNewGameFont != null) buttonNewGame.setFont(buttonNewGameFont);
        buttonNewGame.setForeground(new Color(-16711936));
        buttonNewGame.setText("New Game");
        panelMain.add(buttonNewGame, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(274, 30), null, 0, false));
        final Spacer spacer1 = new Spacer();
        panelMain.add(spacer1, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelMain;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
