package pppp.g1;

import pppp.sim.Point;

/**
 * Wraps a Point and a boolean which determines whether to play music on the way to the point.
 */
public class PointWrapper {
    public Point point;
    public boolean play;

    public PointWrapper(Point point, boolean play) {
        this.point = point;
        this.play = play;
    }
}
