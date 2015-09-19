package pppp.g1;

import pppp.sim.Move;
import pppp.sim.Point;

public class NavUtils {
    /**
     * Coordinates of gate in relative basis.
     * @param id
     * @param side
     */
    public static Point getGetGateRelativeCoordinates(int id, int side) {
        double x = 0;
        double y = 0;
        return new Point(x, y);
    }

    /**
     * Tells us whether the destination is reached from the source or not based on how close the two points are.
     * @param source
     * @param destination
     * @param EPSILON
     */
    public static boolean isDestinationReached(Point source, Point destination, double EPSILON) {
    	if (Math.abs(source.x - destination.x) < EPSILON && Math.abs(source.y - destination.y) < EPSILON) {
            return true;
        }
        return false;
    }

    /**
     * Converts from absolute to relative basis.
     * @param id
     * @param side
     * @param x
     * @param y
     */
    public static Point transformAbsoluteToRelativeCoordinates(int id, int side, double x, double y) {
        double offset = 0.5 * side; // gate is located at midpoint of side
        if (id == 0) {
            return new Point(x, offset - y);
        } else if (id == 1) {
            return new Point(side - y, offset - x);
        } else if (id == 2) {
            return new Point(side - x, y - offset);
        } else {
            return new Point(y - side, x - offset);
        }
    }

    /**
     * Converts from relative to absolute basis.
     * @param id
     * @param side
     * @param x
     * @param y
     */
    public static Point transformRelativeToAbsoluteCoordinates(int id, int side, double x, double y) {
        double offset = 0.5 * side; // gate is located at midpoint of side
        if (id == 0) {
            return new Point(x, offset - y);
        } else if (id == 1) {
            return new Point(side - y, offset - x);
        } else if (id == 2) {
            return new Point(side - x, y + offset);
        } else {
            return new Point(y + side, x + offset);
        }
    }

    /**
     * Create move from source to destination.
     * @param src
     * @param dst
     * @param play
     */
    public static Move creatMove(Point src, Point dst, boolean play) {
        double dx = dst.x - src.x;
        double dy = dst.y - src.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        double limit = play ? 0.1 : 0.5;
        if (length > limit) {
            dx = (dx * limit) / length;
            dy = (dy * limit) / length;
        }
        return new Move(dx, dy, play);
    }
}
