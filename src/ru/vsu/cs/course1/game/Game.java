package ru.vsu.cs.course1.game;

import java.util.Random;

/**
 *
 * Класс, реализующий логику игры
 */
public class Game {
    public enum Colors {
        RED,
        DARK_RED,
        BLUE,
        CYAN,
        MAGENTA,
        GREEN,
        YELLOW
    }

    /**
     * Перечисление, хранящее состояние ячейки;
     */
    public enum CellState {
        NO_BALL,
        BALL_STANDS,
        BALL_JUMPING,
        BALL_WILL_APPEAR
    }

    /**
     * Перечисление, хранящее состояние игры;
     */
    public enum GameState {
        NOT_STARTED,
        PLAYING,
        GAME_OVER
    }

    /**
     * Класс для ячейки;
     */
    public static class BallCell {
        private CellState state;
        private Colors color;

        public BallCell(CellState state) {
            this.state = state;
        }

        public CellState getState() {
            return state;
        }

        public Colors getColor() {
            return color;
        }
    }

    /**
     * Объект Random для генерации случайных чисел;
     */
    private static final Random RND = new Random();

    /**
     * Двумерный массив для лабиринта;
     * Для проверки наличия пути к клетке;
     */
    private int[][] labyrinth;

    /**
     * Двумерный массив из чисел, означающих цвет шариков;
     * Для удаления рядов;
     */
    private static int[][] matrix;

    private static boolean wasRemoteLine = false;
    /**
     * Двумерный массив для хранения игрового поля;
     */
    private BallCell[][] field = null;

    /**
     * Счёт игры;
     */
    private int score = 0;

    public Game() {
    }

    /**
     * Игра пока не запущена;
     */
    private GameState state = GameState.NOT_STARTED;

    /**
     * Массив цветов;
     */
    private static final Colors[] allColors = Colors.values();

    /**
     * Размер массива;
     */
    private final int countOfColors = allColors.length;

    /**
     * Запуск новой игры;
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     */
    public void newGame(int rowCount, int colCount) {
        // создаем поле
        // Каждая ячейка в состоянии NO_BALL
        field = new BallCell[rowCount][colCount];
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                field[r][c] = new BallCell(CellState.NO_BALL);
            }
        }

        //Раставляем 5 шаров в состоянии BALL_STANDS
        for (int b = 0; b < 5; b++) {
            int cellIndex = RND.nextInt(rowCount * colCount - b);
            int k = 0;
            standBalls:
            for (int r = 0; r < rowCount; r++) {
                for (int c = 0; c < colCount; c++) {
                    if (field[r][c].state != CellState.BALL_STANDS) {
                        if (k == cellIndex) {
                            field[r][c].state = CellState.BALL_STANDS;
                            field[r][c].color = allColors[RND.nextInt(countOfColors)];
                            break standBalls;
                        }
                        k++;
                    }
                }
            }
        }

        //Расставляем 3 шара в состоянии BALL_WILL_APPEAR
        standFutureBalls(rowCount, colCount, 3);

        //На двумерном массиве целых чисел в ячейках, где есть шарик ставим -1. Где его нет - 0;
        labyrinth = editWalls(rowCount, colCount);
        //На матрице ставим ID цвета там, где стоит шарик на поле.
        matrix = editMatrix(rowCount, colCount, allColors);
        score = 0;
        state = GameState.PLAYING;
    }

    /**
     * Нажатие левой кнопки мыши по ячейке;
     * @param row номер строки;
     * @param col номер ячейки;
     */
    public void leftMouseClick(int row, int col) {
        int rowCount = getRowCount(), colCount = getColCount();
        if (state != GameState.PLAYING || row < 0 || row >= rowCount || col < 0 || col >= colCount) {
            return;
        }

        BallCell cell = field[row][col];
        switch (cell.state) {
            case BALL_STANDS:
                if (!findJumpingBall()) {
                    cell.state = CellState.BALL_JUMPING;
                    labyrinth[row][col] = 0;
                }
                else if (findJumpingBall()) {
                    int[] cordsOfJumpingBall = findCordsOfJumpingBall();
                    int rowJump = cordsOfJumpingBall[0], colJump = cordsOfJumpingBall[1];

                    field[rowJump][colJump].state = CellState.BALL_STANDS;
                    labyrinth[rowJump][colJump] = -1;
                    cell.state = CellState.BALL_JUMPING;
                    labyrinth[row][col] = 0;
                }
                break;

            case BALL_JUMPING:
                cell.state = CellState.BALL_STANDS;
                labyrinth[row][col] = -1;
                break;

            case NO_BALL:
                if (!findJumpingBall()) { //проверка, есть ли на поле прыгающие шарики
                    return; //если нет
                }
                else if (findJumpingBall()) { //если есть
                    int[] cordsOfJumpingBall = findCordsOfJumpingBall(); //находим координатые прыгающего шарика
                    int rowJump = cordsOfJumpingBall[0], colJump = cordsOfJumpingBall[1];
                    BallCell JumpingBall = field[rowJump][colJump];

                    PathFinding.mazeDfsRecurs(labyrinth, rowJump, colJump);
                    if (labyrinth[row][col] != 0) { //перемещение, если есть путь к ячейке
                        cell.state = CellState.BALL_STANDS;
                        cell.color = JumpingBall.color;
                        matrix[row][col] = numberOfColor(cell.color);
                        JumpingBall.state = CellState.NO_BALL;
                        JumpingBall.color = null; //ячейке, которую выбрали, присваиваем наличие ячейки и цвет, А там, где был мячик - удаляем его.
                        matrix[rowJump][colJump] = -1;
                    } else {
                        return;
                    }

                    for (int r = 0; r < rowCount; r++) {
                        for (int c = 0; c < colCount; c++) {
                            if (field[r][c].state == CellState.BALL_WILL_APPEAR) {
                                field[r][c].state = CellState.BALL_STANDS;
                                matrix[r][c] = numberOfColor(field[r][c].color);
                            }
                        }
                    } //Все BALL_WILL_APPEAR заменяем на BALL_STANDS

                    remoteLines(rowCount, colCount);

                    score += editField(rowCount, colCount);

                    //Ставим рандомно шарики, которые должны в будущем появиться

                    int nullCell = countNullCell(rowCount, colCount);
                    if (nullCell >= 3) {
                        standFutureBalls(rowCount, colCount, 3);
                    } else if (nullCell > 0 && nullCell < 3) {
                        standFutureBalls(rowCount, colCount, nullCell);
                    } else {
                        state = GameState.GAME_OVER;
                        return;
                    }

                    labyrinth = editWalls(rowCount, colCount);
                }
                break;
            case BALL_WILL_APPEAR:
                if (!findJumpingBall()) {
                    return;
                }
                else if (findJumpingBall()) {
                    int nullCell = countNullCell(rowCount, colCount);

                    int[] cordsOfJumpingBall = findCordsOfJumpingBall();
                    int rowJump = cordsOfJumpingBall[0], colJump = cordsOfJumpingBall[1];
                    BallCell jumpingBall = field[rowJump][colJump];

                    PathFinding.mazeDfsRecurs(labyrinth, rowJump, colJump);
                    if (labyrinth[row][col] != 0) { //перемещение

                        if (nullCell == 0 && countWillAppear(rowCount, colCount) == 1) {
                            Colors color = cell.color;

                            cell.color = jumpingBall.color;
                            jumpingBall.color = color;

                            cell.state = CellState.BALL_STANDS;
                            jumpingBall.state = CellState.BALL_STANDS;

                            state = GameState.GAME_OVER;
                            return;
                        }

                        int[] cords = cordsForMoveBall(rowCount,colCount);
                        field[cords[0]][cords[1]].state = CellState.BALL_STANDS;
                        field[cords[0]][cords[1]].color = cell.color;
                        labyrinth[cords[0]][cords[1]] = -1;

                        cell.state = CellState.NO_BALL;
                        cell.color = null;
                        labyrinth[row][col] = 0;

                        cell.state = CellState.BALL_STANDS;
                        cell.color = jumpingBall.color;
                        matrix[row][col] = numberOfColor(cell.color);
                        jumpingBall.state = CellState.NO_BALL;
                        jumpingBall.color = null;
                        matrix[rowJump][colJump] = -1;
                    } else {
                        return;
                    }

                    for (int r = 0; r < rowCount; r++) {
                        for (int c = 0; c < colCount; c++) {
                            if (field[r][c].state == CellState.BALL_WILL_APPEAR) {
                                field[r][c].state = CellState.BALL_STANDS;
                                matrix[r][c] = numberOfColor(field[r][c].color);
                            }
                        }
                    }

                    remoteLines(rowCount, colCount);

                    score += editField(rowCount, colCount);

                    if (nullCell >= 3) {
                        standFutureBalls(rowCount, colCount, 3);
                    } else if (nullCell > 0 && nullCell < 3) {
                        standFutureBalls(rowCount, colCount, nullCell);
                    } else {
                        state = GameState.GAME_OVER;
                        return;
                    }

                    labyrinth = editWalls(rowCount, colCount);
                }
                break;
        }
    }

    /**
     * Расставляет шарики в состоянии BALL_WILL_APPEAR;
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     */
    private void standFutureBalls(int rowCount, int colCount, int countBalls) {

        for (int b = 0; b < countBalls; b++) {
            int cellIndex = RND.nextInt(countNullCell(rowCount, colCount));
            int k = 0;
            standBalls:
            for (int r = 0; r < rowCount; r++) {
                for (int c = 0; c < colCount; c++) {
                    if (field[r][c].state == CellState.NO_BALL) {
                        if (k == cellIndex) {
                            field[r][c].state = CellState.BALL_WILL_APPEAR;
                            field[r][c].color = allColors[RND.nextInt(countOfColors)];
                            break standBalls;
                        }
                        k++;
                    }
                }
            }
        }
    }

    /**
     *
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     * @return координаты для перемещения шарика BALL_WILL_APPEAR, если его ячейку занял шарик.
     */
    private int[] cordsForMoveBall(int rowCount, int colCount) {
        int[] cords = new int[2];
        int cellIndex = RND.nextInt(countNullCell(rowCount, colCount) + 1);
        int k = 0;
        standBalls:
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (field[r][c].state == CellState.NO_BALL) {
                    if (k == cellIndex) {
                        cords[0] = r;
                        cords[1] = c;
                        break standBalls;
                    }
                    k++;
                }
            }
        }
        return cords;
    }

    /**
     *
     * @param color цвет;
     * @return номер цвета;
     */
    private int numberOfColor(Colors color) {
        int number = -1;
        for (int i = 0; i < allColors.length; i++) {
            if (color == allColors[i]) {
                number = i;
            }
        }
        return number;
    }
    /**
     *
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     * @return количество ячеек NO_BALL;
     */
    private int countNullCell(int rowCount, int colCount) {
        int count = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (field[r][c].state == CellState.NO_BALL) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     *
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     * @return количество шариков BALL_WILL_APPEAR на поле;
     */
    private int countWillAppear(int rowCount, int colCount) {
        int count = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (field[r][c].state == CellState.BALL_WILL_APPEAR) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * @return false - если шарика нет;
     */
    private boolean findJumpingBall() {
        for (int r = 0; r < getRowCount(); r++) {
            for (int c = 0; c < getColCount(); c++) {
                if (field[r][c].state == CellState.BALL_JUMPING) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @return Координаты прыгающего шарика;
     */
    private int[] findCordsOfJumpingBall() {
        int[] rowAndCol = new int[2];
        for (int r = 0; r < getRowCount(); r++) {
            for (int c = 0; c < getColCount(); c++) {
                if (field[r][c].state == CellState.BALL_JUMPING) {
                    rowAndCol[0] = r;
                    rowAndCol[1] = c;
                    break;
                }
            }
        }
        return rowAndCol;
    }

    /**
     *
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     * @return двумерный массив из чисел. -1 стоит там, где есть шарик, 0 - где шарика нет.
     */
    private int[][] editWalls(int rowCount, int colCount) {
        int[][] maze = new int[rowCount][colCount];
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (field[r][c].state == CellState.BALL_STANDS) {
                    maze[r][c] = -1;
                } else if (field[r][c].state != CellState.BALL_STANDS) {
                    maze[r][c] = 0;
                }
            }
        }
        return maze;
    }

    /**
     *
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     * @param colors массив цветов;
     * @return -1 - если в ячейке не стоит шарик, 0-6 - если шарик стоит в ячейке;
     */
    private int[][] editMatrix(int rowCount, int colCount, Colors[] colors) {
        int[][] mat = new int[rowCount][colCount];
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (field[r][c].state == CellState.NO_BALL) {
                    mat[r][c] = -1;
                }
                else if (field[r][c].state == CellState.BALL_STANDS || field[r][c].state == CellState.BALL_JUMPING) {
                    for (int i = 0; i < colors.length; i++) {
                        if (field[r][c].color == colors[i]) {
                            mat[r][c] = i;
                        }
                    }
                } else if (field[r][c].state == CellState.BALL_WILL_APPEAR) {
                    mat[r][c] = -2;
                }
            }
        }
        return mat;
    }

    /**
     * Изменяет field в зависимости от matrix;
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     * @return количество удалённых шариков;
     */
    private int editField(int rowCount, int colCount) {
        int count = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (field[r][c].state == CellState.BALL_STANDS && matrix[r][c] == -1) {
                    field[r][c].state = CellState.NO_BALL;
                    field[r][c].color = null;
                    count++;
                }
            }
        }
        if (count >= 5) {
            return count;
        }
        return 0;
    }

    /**
     * Удаляет все линии на матрице;
     * @param rowCount количество строк;
     * @param colCount количество столбцов;
     */
    private static void remoteLines(int rowCount, int colCount) {
        int countUp = 0;
        int countRightAndUp = 0;
        int countRight = 0;
        int countRightAndDown = 0;
        int countDown = 0;
        int countLeftAndDown = 0;
        int countLeft = 0;
        int countLeftAndUp = 0;

        int cordR = 0, cordC = 0;
        int numberOfRoute = 0;
        int countForDeleteLastElement = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                if (r == 0 && c == 0) { //1
                    if (matrix[r][c] != -1) {
                        countRight = remoteRight(r, c, colCount);
                        if (countRight != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 3;
                            countForDeleteLastElement = countRight;
                        }
                        countRightAndDown = remoteRightAndDown(r, c, rowCount, colCount);
                        if (countRightAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 4;
                            countForDeleteLastElement = countRightAndDown;
                        }
                        countDown = remoteDown(r, c, rowCount);
                        if (countDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 5;
                            countForDeleteLastElement = countDown;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r == 0 && c != 0 && c != colCount - 1) {//2
                    if (matrix[r][c] != -1) {
                        countRight = remoteRight(r, c, colCount);
                        if (countRight != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 3;
                            countForDeleteLastElement = countRight;
                        }
                        countRightAndDown = remoteRightAndDown(r, c, rowCount, colCount);
                        if (countRightAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 4;
                            countForDeleteLastElement = countRightAndDown;
                        }
                        countDown = remoteDown(r, c, rowCount);
                        if (countDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 5;
                            countForDeleteLastElement = countDown;
                        }
                        countLeftAndDown = remoteLeftAndDown(r, c, rowCount);
                        if (countLeftAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 6;
                            countForDeleteLastElement = countLeftAndDown;
                        }
                        countLeft = remoteLeft(r, c);
                        if (countLeft != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 7;
                            countForDeleteLastElement = countLeft;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r == 0 && c == colCount - 1) {//3
                    if (matrix[r][c] != -1) {
                        countDown = remoteDown(r, c, rowCount);
                        if (countDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 5;
                            countForDeleteLastElement = countDown;
                        }
                        countLeftAndDown = remoteLeftAndDown(r, c, rowCount);
                        if (countLeftAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 6;
                            countForDeleteLastElement = countLeftAndDown;
                        }
                        countLeft = remoteLeft(r, c);
                        if (countLeft != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 7;
                            countForDeleteLastElement = countLeft;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r != 0 && c == colCount - 1 && r != rowCount - 1) {//4
                    if (matrix[r][c] != -1) {
                        countUp = remoteUp(r, c);
                        if (countUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 1;
                            countForDeleteLastElement = countUp;
                        }
                        countDown = remoteDown(r, c, rowCount);
                        if (countDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 5;
                            countForDeleteLastElement = countDown;
                        }
                        countLeftAndDown = remoteLeftAndDown(r, c, rowCount);
                        if (countLeftAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 6;
                            countForDeleteLastElement = countLeftAndDown;
                        }
                        countLeft = remoteLeft(r, c);
                        if (countLeft != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 7;
                            countForDeleteLastElement = countLeft;
                        }
                        countLeftAndUp = remoteLeftAndUp(r, c);
                        if (countLeftAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 8;
                            countForDeleteLastElement = countLeftAndUp;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r == rowCount - 1 && c == colCount - 1) {//5
                    if (matrix[r][c] != -1) {
                        countUp = remoteUp(r, c);
                        if (countUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 1;
                            countForDeleteLastElement = countUp;
                        }
                        countLeft = remoteLeft(r, c);
                        if (countLeft != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 7;
                            countForDeleteLastElement = countLeft;
                        }
                        countLeftAndUp = remoteLeftAndUp(r, c);
                        if (countLeftAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 8;
                            countForDeleteLastElement = countLeftAndUp;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r == rowCount - 1 && c != 0 && c != colCount - 1) {//6
                    if (matrix[r][c] != -1) {
                        countUp = remoteUp(r, c);
                        if (countUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 1;
                            countForDeleteLastElement = countUp;
                        }
                        countRightAndUp = remoteRightAndUp(r, c, colCount);
                        if (countRightAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 2;
                            countForDeleteLastElement = countRightAndUp;
                        }
                        countRight = remoteRight(r, c, colCount);
                        if (countRight != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 3;
                            countForDeleteLastElement = countRight;
                        }
                        countLeft = remoteLeft(r, c);
                        if (countLeft != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 7;
                            countForDeleteLastElement = countLeft;
                        }
                        countLeftAndUp = remoteLeftAndUp(r, c);
                        if (countLeftAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 8;
                            countForDeleteLastElement = countLeftAndUp;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r == rowCount - 1 && c == 0) { //7
                    if (matrix[r][c] != -1) {
                        countUp = remoteUp(r, c);
                        if (countUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 1;
                            countForDeleteLastElement = countUp;
                        }
                        countRightAndUp = remoteRightAndUp(r, c, colCount);
                        if (countRightAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 2;
                            countForDeleteLastElement = countRightAndUp;
                        }
                        countRight = remoteRight(r, c, colCount);
                        if (countRight != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 3;
                            countForDeleteLastElement = countRight;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r != 0 && r != rowCount - 1 && c == 0) { //8
                    if (matrix[r][c] != -1) {
                        countUp = remoteUp(r, c);
                        if (countUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 1;
                            countForDeleteLastElement = countUp;
                        }
                        countRightAndUp = remoteRightAndUp(r, c, colCount);
                        if (countRightAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 2;
                            countForDeleteLastElement = countRightAndUp;
                        }
                        countRight = remoteRight(r, c, colCount);
                        if (countRight != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 3;
                            countForDeleteLastElement = countRight;
                        }
                        countRightAndDown = remoteRightAndDown(r, c, rowCount, colCount);
                        if (countRightAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 4;
                            countForDeleteLastElement = countRightAndDown;
                        }
                        countDown = remoteDown(r, c, rowCount);
                        if (countDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 5;
                            countForDeleteLastElement = countDown;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                } else if (r > 0 && c > 0 && r < rowCount - 1 && c < colCount - 1) { //9
                    if (matrix[r][c] != -1) {
                        countUp = remoteUp(r, c);
                        if (countUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 1;
                            countForDeleteLastElement = countUp;
                        }
                        countRightAndUp = remoteRightAndUp(r, c, colCount);
                        if (countRightAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 2;
                            countForDeleteLastElement = countRightAndUp;
                        }
                        countRight = remoteRight(r, c, colCount);
                        if (countRight != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 3;
                            countForDeleteLastElement = countRight;
                        }
                        countRightAndDown = remoteRightAndDown(r, c, rowCount, colCount);
                        if (countRightAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 4;
                            countForDeleteLastElement = countRightAndDown;
                        }
                        countDown = remoteDown(r, c, rowCount);
                        if (countDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 5;
                            countForDeleteLastElement = countDown;
                        }
                        countLeftAndDown = remoteLeftAndDown(r, c, rowCount);
                        if (countLeftAndDown != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 6;
                            countForDeleteLastElement = countLeftAndDown;
                        }
                        countLeft = remoteLeft(r, c);
                        if (countLeft != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 7;
                            countForDeleteLastElement = countLeft;
                        }
                        countLeftAndUp = remoteLeftAndUp(r, c);
                        if (countLeftAndUp != 0) {
                            cordR = r;
                            cordC = c;
                            numberOfRoute = 8;
                            countForDeleteLastElement = countLeftAndUp;
                        }
                    }
                    if (wasRemoteLine) {
                        matrix[r][c] = -1;
                        wasRemoteLine = false;
                    }
                }
            }
        }
        if (numberOfRoute == 1) {
            matrix[cordR - countForDeleteLastElement][cordC] = -1;
        } else if (numberOfRoute == 2) {
            matrix[cordR - countForDeleteLastElement][cordC + countForDeleteLastElement] = -1;
        } else if (numberOfRoute == 3) {
            matrix[cordR][cordC + countForDeleteLastElement] = -1;
        } else if (numberOfRoute == 4) {
            matrix[cordR + countForDeleteLastElement][cordC + countForDeleteLastElement] = -1;
        } else if (numberOfRoute == 5) {
            matrix[cordR + countForDeleteLastElement][cordC] = -1;
        } else if (numberOfRoute == 6) {
            matrix[cordR + countForDeleteLastElement][cordC - countForDeleteLastElement] = -1;
        } else if (numberOfRoute == 7) {
            matrix[cordR][cordC - countForDeleteLastElement] = -1;
        } else if (numberOfRoute == 8) {
            matrix[cordR - countForDeleteLastElement][cordC - countForDeleteLastElement] = -1;
        }
    }

    private static int remoteUp(int row, int col) {
        int count = 0;
        for (int r = row; r >= 1; r--) {
            if (matrix[r][col] == matrix[r - 1][col]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int index = row - 1; index > row - count ; index--) {
                        matrix[index][col] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int index = row - 1; index > row - count ; index--) {
                matrix[index][col] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    private static int remoteRightAndUp(int row, int col, int colCount) {
        int count = 0;
        for (int c = col, r = row; c < colCount - 1 && r >= 1; c++, r--) {
            if (matrix[r][c] == matrix[r - 1][c + 1]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int indexR = row - 1, indexC = col + 1; indexR > row - count && indexC < col + count; indexR--, indexC++) {
                        matrix[indexR][indexC] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int indexR = row - 1, indexC = col + 1; indexR > row - count && indexC < col + count; indexR--, indexC++) {
                matrix[indexR][indexC] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    private static int remoteRight(int row, int col, int colCount) {
        int count = 0;
        for (int c = col; c < colCount - 1; c++) {
            if (matrix[row][c] == matrix[row][c + 1]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int index = col + 1; index < col + count; index++) {
                        matrix[row][index] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int index = col + 1; index < col + count; index++) {
                matrix[row][index] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    private static int remoteRightAndDown(int row, int col, int rowCount, int colCount) {
        int count = 0;
        for (int r = row, c = col; r < rowCount - 1 && c < colCount - 1; r++, c++) {
            if (matrix[r][c] == matrix[r + 1][c + 1]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int indexR = row + 1, indexC = col + 1; indexR < row + count && indexC < col + count; indexR++, indexC++) {
                        matrix[indexR][indexC] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int indexR = row + 1, indexC = col + 1; indexR < row + count && indexC < col + count; indexR++, indexC++) {
                matrix[indexR][indexC] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    private static int remoteDown(int row, int col, int rowCount) {
        int count = 0;
        for (int r = row; r < rowCount - 1; r++) {
            if (matrix[r][col] == matrix[r + 1][col]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int index = row + 1 ; index < row + count; index++) {
                        matrix[index][col] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int index = row + 1 ; index < row + count; index++) {
                matrix[index][col] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    private static int remoteLeftAndDown(int row, int col, int rowCount) {
        int count = 0;
        for (int r = row, c = col; r < rowCount - 1 && c > 1; r++, c--) {
            if (matrix[r][c] == matrix[r + 1][c - 1]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int indexR = row + 1, indexC = col - 1; indexR < row + count && indexC > col - count; indexR++, indexC--) {
                        matrix[indexR][indexC] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int indexR = row + 1, indexC = col - 1; indexR < row + count && indexC > col - count; indexR++, indexC--) {
                matrix[indexR][indexC] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    private static int remoteLeft(int row, int col) {
        int count = 0;
        for (int c = col; c >= 1; c--) {
            if (matrix[row][c] == matrix[row][c - 1]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int index = col - 1; index > col - count; index--) {
                        matrix[row][index] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int index = col - 1; index > col - count; index--) {
                matrix[row][index] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    private static int remoteLeftAndUp(int row, int col) {
        int count = 0;
        for (int r = row, c = col; r >= 1 && c >= 1; r--, c--) {
            if (matrix[r][c] == matrix[r - 1][c - 1]) {
                count++;
            } else {
                if (count >= 4) {
                    for (int indexR = row - 1, indexC = col - 1; indexR > row - count && indexC > col - count; indexR--, indexC--) {
                        matrix[indexR][indexC] = -1;
                    }
                    wasRemoteLine = true;
                    return count;
                }
                count = 0;
                return count;
            }
        }
        if (count >= 4) {
            for (int indexR = row - 1, indexC = col - 1; indexR > row - count && indexC > col - count; indexR--, indexC--) {
                matrix[indexR][indexC] = -1;
            }
            wasRemoteLine = true;
        } else {
            count = 0;
        }
        return count;
    }

    public int getRowCount() {
        return field == null ? 0 : field.length;
    }

    public int getColCount() {
        return field == null ? 0 : field[0].length;
    }

    public GameState getState() {
        return state;
    }

    public BallCell getCell(int row, int col) {
        return (field == null ||
                row < 0 || row >= getRowCount() ||
                col < 0 || col >= getColCount()) ? null : field[row][col];
    }

    public int getScore() {
        return score;
    }
}