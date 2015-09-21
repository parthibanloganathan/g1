package pppp.g1;

import pppp.sim.Point;

public class Grid {
    public double cellSize;
    public int side;
    public Cell[][] grid; // rows x columns

    /**
     * Create a grid of square cells each of side length size.
     *
     * @param side   Side of the grid.
     * @param slices Number of devisions by which to devide the side.
     */
    public Grid(int side, int slices) {
        // The board consists of size^2 number of square cells.
        this.side = side;
        cellSize = (double) side / slices;
        double offset = (double) side / 2;
        this.grid = new Cell[slices][slices];
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < slices; j++) {
                this.grid[i][j] = new Cell(
                        new Point(  // X, Y - bottom-left corner
                                (i * cellSize) - offset,
                                (j * cellSize) - offset
                        ),
                        new Point(  // X, Y - center
                                (i + 0.5) * cellSize - offset,
                                (j + 0.5) * cellSize - offset
                        ),
                        cellSize, 0 // initialize with zero weight
                );
            }
        }
    }

    /**
     * Update the weights of all cells.
     *
     * @param pipers        The positions of all the pipers on the board.
     * @param pipers_played The state of playing music for all the pipers.
     * @param rats          The positions for all rats on the board.
     */
    public void updateCellWeights(
            Point[][] pipers, boolean[][] pipers_played, Point[] rats
    ) {
        // Reset cell weights
        for (Cell[] row : this.grid) {
            for (Cell cell : row) {
                cell.weight = 0;
            }
        }

        // Compute each cell's weight
        for (Point rat : rats) {
            Cell cell = getCellContainingPoint(rat);
            if (cell != null) {
                cell.weight++;
            }
        }
    }

    /**
     * Find the cell containing the given point.
     *
     * @param point The position for which the cell needs to be found.
     */
    private Cell getCellContainingPoint(Point point) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                double left = cell.corner.x;
                double bottom = cell.corner.y;
                double top = cell.corner.y + cell.size;
                double right = cell.corner.x + cell.size;

                if (point.y >= bottom && point.y < top && point.x >= left &&
                        point.x < right) {
                    return cell;
                }
            }
        }
        return null;
    }
}
