public final class MineCounter {

    private MineCounter() {
        // Utility class
    }

    public static int countAdjacentMines(
            boolean[][] mines, int row, int col) {

        if (mines == null || mines.length == 0 ||
                mines[0] == null || mines[0].length == 0) {
            throw new IllegalArgumentException("Invalid minefield");
        }

        int rows = mines.length;
        int cols = mines[0].length;

        for (boolean[] mineRow : mines) {
            if (mineRow == null || mineRow.length != cols) {
                throw new IllegalArgumentException(
                    "Minefield must be rectangular"
                );
            }
        }

        if (row < 0 || row >= rows ||
                col < 0 || col >= cols) {
            throw new IllegalArgumentException(
                "Cell is outside the minefield"
            );
        }

        int count = 0;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {

                if (dr == 0 && dc == 0) {
                    continue;
                }

                int newRow = row + dr;
                int newCol = col + dc;

                if (newRow >= 0 && newRow < rows &&
                        newCol >= 0 && newCol < cols &&
                        mines[newRow][newCol]) {
                    count++;
                }
            }
        }

        return count;
    }
}
