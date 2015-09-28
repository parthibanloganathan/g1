package pppp.g1;

import java.util.ArrayList;
import java.util.Collections;
import pppp.sim.Point;

public class Cluster implements Comparable<Cluster> {

    public ArrayList<Point> points;
    public Point centroid;
    public int id;
    public int weight;

    //Creates a new Cluster
    public Cluster(int id) {
        this.id = id;
        this.points = new ArrayList<Point>();
        this.centroid = null;
        this.weight = 0;
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public void setPoints(Point[] points) {
        this.points = new ArrayList<Point>();
        Collections.addAll(this.points, points);
    }

    public Point getCentroid() {
        return centroid;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }

    public int getId() {
        return id;
    }

    public void clear() {
        points.clear();
    }
    
    public double getClusterPointsDistance() {
    	double radius = 0;
    	for(Point p : points) {
    		radius += Utils.distance(centroid, p);
    	}
    	return radius;
    }

    @Override
    public Cluster clone() {
        Cluster ret = new Cluster(this.id);
        ret.points = this.points;
        ret.centroid = new Point(this.centroid.x, this.centroid.y);
        ret.weight = this.weight;
        return ret;
    }

    public int compareTo(Cluster rhs) {
        return -Integer.compare(this.weight, rhs.weight);
    }
}