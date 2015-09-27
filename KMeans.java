/**
 * The logic for the k-means algorithm was adopted from:
 * http://www.dataonfocus.com/k-means-clustering-java-code/
 * but changed to apply to this particular project.
 */
package pppp.g1;

import pppp.sim.Point;

import java.util.ArrayList;
import java.util.Collections;

public class KMeans {
    private int half;
    private ArrayList<Point> points;
    private ArrayList<Cluster> clusters;

    public KMeans(int side) {
        this.half = side / 2;
        this.points = null;
        this.clusters = null;
    }

    //    public static void main(String[] args) {
//
//        KMeans kmeans = new KMeans();
//        kmeans.init();
//        kmeans.calculate();
//    }
//
//    //Initializes the process
    public void init(Point[] points, int K) {
        this.points = new ArrayList<Point>();
        Collections.addAll(this.points, points);

        this.clusters = new ArrayList<Cluster>();
        for (int i = 0; i < K; i++) {
            Cluster cluster = new Cluster(i);
            double x = (-half) + (float) (Math.random() * (2 * half) + 1);
            double y = (-half) + (float) (Math.random() * (2 * half) + 1);
            Point centroid = new Point(x, y);
            cluster.setCentroid(centroid);
            clusters.add(cluster);
        }
    }

//    private void plotClusters() {
//        for (int i = 0; i & lt; NUM_CLUSTERS; i++){
//            Cluster c = clusters.get(i);
//            c.plotCluster();
//        }
//    }

    public ArrayList<Cluster> getClusters() {
        return clusters;
//        ArrayList<Cluster> ret = new ArrayList<Cluster>();
//        for (Cluster clust : clusters) {
//            ret.add(clust.clone());
//        }
//        return ret;
    }

    public void calculate() {
        boolean finish = false;
        int j = 0;

        while (!finish) {
            // Clear all associated points for last clustering.
            clearClusters();

            // Obtain latest centroids.
            ArrayList<Point> lastCentroids = getCentroids();

            //Assign points to the closer cluster
            assignCluster();

            //Calculate new centroids.
            calculateCentroids();

            j++;

            ArrayList<Point> currentCentroids = getCentroids();

            //Calculates total distance between new and old Centroids
            double distance = 0;
            for (int i = 0; i < lastCentroids.size(); i++) {
                distance += Utils.distance(lastCentroids.get(i), currentCentroids.get(i));
            }
            if (distance < 1) {
                finish = true;
            }
        }
    }

    private void clearClusters() {
        for (Cluster cluster : clusters) {
            cluster.clear();
        }
    }

    private ArrayList<Point> getCentroids() {
        ArrayList<Point> centroids = new ArrayList<Point>();
        for (Cluster cluster : clusters) {
            Point aux = cluster.getCentroid();
            Point point = new Point(aux.x, aux.y);
            centroids.add(point);
        }
        return centroids;
    }

    private void assignCluster() {
        int cluster_id = 0;
        for (Point point : points) {
            double min = Double.MAX_VALUE;
            for (int i = 0; i < clusters.size(); i++) {
                Cluster c = clusters.get(i);
                double distance = Utils.distance(point, c.getCentroid());
                if (distance < min) {
                    min = distance;
                    cluster_id = i;
                }
            }
            clusters.get(cluster_id).addPoint(point);
        }
    }

    private void calculateCentroids() {
        for (Cluster cluster : clusters) {
            double sumX = 0;
            double sumY = 0;
            ArrayList<Point> list = cluster.getPoints();
            int n_points = list.size();

            for (Point point : list) {
                sumX += point.x;
                sumY += point.y;
            }

            if (n_points > 0) {
                double newX = sumX / n_points;
                double newY = sumY / n_points;
                Point centroid = new Point(newX, newY);
                cluster.setCentroid(centroid);
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
    public void updateClusterWeights(
            Point[][] pipers, boolean[][] pipers_played, Point[] rats
    ) {
        for (Cluster cluster : clusters) {
            cluster.weight = 0;
        }

        // Compute each cluster's weight
        for (Point rat : rats) {
            Cluster cluster = getClusterContainingPoint(rat);
            if (cluster != null) {
                cluster.weight++;
            }
        }
    }

    /**
     * Find the cell containing the given point.
     *
     * @param point The position for which the cell needs to be found.
     */
    private Cluster getClusterContainingPoint(Point point) {
        for (Cluster cluster : clusters) {
            ArrayList<Point> list = cluster.getPoints();
            if (list.contains(point)) {
                return cluster;
            }
        }
        return null;
//
//        for (Cell[] row : grid) {
//            for (Cell cell : row) {
//                double left = cell.corner.x;
//                double bottom = cell.corner.y;
//                double top = cell.corner.y + cell.size;
//                double right = cell.corner.x + cell.size;
//
//                if (point.y >= bottom && point.y < top && point.x >= left &&
//                        point.x < right) {
//                    return cell;
//                }
//            }
//        }
//        return null;
    }
}