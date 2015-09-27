package pppp.g1;

import pppp.sim.Point;

public class PointWeight implements Comparable<PointWeight> {
    public int id;
    public Point point;
    public double weight;

    public PointWeight(int id, Point point, double weight) {
        this.id = id;
        this.point = point;
        this.weight = weight;
    }

    public int compareTo(PointWeight other) {
        return Double.compare(weight, other.weight);
    }
}
