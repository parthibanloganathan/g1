package pppp.g1;

import pppp.sim.Point;
import java.util.ArrayList;

public class PiperGroup {
	ArrayList<Integer> pipers;
	Point centroid;
	int id;
	
	public PiperGroup(int id) {
		this.id = id;
		pipers = new ArrayList<Integer>();
		centroid = null;
	}
	
	public void addPiper(Integer piperID) {
		pipers.add(piperID);
	}
	
	public Point recalculateCentroid(Point[][] all_pipers) {
		Point[] ourPipers = all_pipers[id];
		double sum_x = 0;
		double sum_y = 0;
		for(Integer piperID : pipers) {
			sum_x += ourPipers[piperID].x;
			sum_y += ourPipers[piperID].y;
		}
		if(pipers.size() > 0) {
			double x = sum_x / pipers.size();
			double y = sum_y / pipers.size();
			centroid = new Point(x,y);
		}
		return centroid;
	}
}
