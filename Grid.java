package pppp.g1;

import pppp.sim.Point;

public class Grid {
    public double cellSize;
    public int side;
    public Cell[][] grid; // rows x columns

    /**
     * Create a grid of square cells each of side length size.
     *
     * @param side
     * @param slices
     */
    public Grid(int side, int cellSize) {
        // The board consists of size^2 number of square cells.
        this.cellSize = cellSize;
        int slices = side/cellSize;
        this.side = side;
        this.grid = new Cell[slices][slices];
        int offset = -side/2;
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < slices; j++) {
                this.grid[i][j] = new Cell(
                        new Point(i * cellSize + offset, j * cellSize + offset), // top-left corner
                        new Point((i + 0.5) * cellSize + offset, (j + 0.5) * cellSize + offset), // center
                        cellSize,
                        0 // initialize with zero weight
                );
            }
        }
    }

    /**
     * Update the weights of all cells.
     *
     * @param pipers
     * @param pipers_played
     */
    public void updateCellWeights(Point[][] pipers, boolean[][] pipers_played, Point[] rats) {
        // Reset cell weights
        for(Cell[] row : this.grid) {
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
     * @param point
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
    	/*
        int cellRow = (int) (point.x / cellSize);
        int cellColumn = (int) (point.y / cellSize);
        return grid[cellRow][cellColumn];
        */
    }
}
