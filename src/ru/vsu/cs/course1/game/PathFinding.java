package ru.vsu.cs.course1.game;

public class PathFinding {

    /**
     * Непосредственно реализация рекурсивного обхода лабиринта (поиска пути)
     *
     * Клетки, до которых можно дойти, будут заполнены положительными числами - номерами шагов, на которых
     * дойдем до клетки (1 - стартовая клетка).
     *
     * @param maze Лабиринт (отрицательное число - препятствие), будет заполнено числами - номерами шагов)
     * @param row Номер строки для очередной посещаемой клетки
     * @param col Номер столбца для очередной посещаемой клетки
     * @param step Номер шага
     */
    private static void mazeDfsRecursInner(
            int[][] maze,
            int row, int col,
            int step
    ) {
        int rowCount = maze.length;
        int colCount = maze[0].length;

        if (row < 0 || row >= rowCount ||
                col < 0 || col >= colCount) {
            return;
        }
        if (maze[row][col] != 0) {
            return;
        }

        maze[row][col] = step;
        mazeDfsRecursInner(maze, row - 1, col, step + 1);
        mazeDfsRecursInner(maze, row, col - 1, step + 1);
        mazeDfsRecursInner(maze, row, col + 1, step + 1);
        mazeDfsRecursInner(maze, row + 1, col, step + 1);
    }

    /**
     * Рекурсивный обход лабиринта (поиска пути) - обход в ширину
     *
     * Клетки, до которых можно дойти, будут заполнены положительными числами - номерами шагов, на которых
     * дойдем до клетки (1 - стартовая клетка).
     *
     * @param maze Лабиринт (отрицательное число - препятствие), будет заполнено числами - номерами шагов)
     * @param startCol Номер строки стартовой клетки
     * @param startRow Номер столбца стартовой клетки
     */
    public static void mazeDfsRecurs(
            int[][] maze,
            int startRow, int startCol
    ) {
        mazeDfsRecursInner(maze, startRow, startCol, 1);
    }
}