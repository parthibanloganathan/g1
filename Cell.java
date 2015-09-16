package pppp.g1;

import pppp.sim.Point;

public class Cell implements Comparable<Cell> {
    // corner is the coordinates of the top-left corner of the cell.
    public Point corner = null;
    public Point center = null;
    public float size = 0;
    public int weight = 0;

    Cell() {
        this.corner = new Point(0.0, 0.0);
        this.center = new Point(0.0, 0.0);
    }

    Cell(Point corner, Point center, float size, int weight) {
        this.corner = corner;
        this.center = center;
        this.size = size;
        this.weight = weight;
    }

    public int compareTo(Cell other) {
        return Integer.compare(this.weight, other.weight);
    }
}
