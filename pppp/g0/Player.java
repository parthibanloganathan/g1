package pppp.g0;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    private int[] pos_index = null;
    private Point[][] pos = null;
    private Point[] random_pos = null;
    private Random gen = new Random();
    private Cell[][] grid = null;

    // create move towards specified destination
    private static Move move(Point src, Point dst, boolean play) {
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

    // generate point after negating or swapping coordinates
    private static Point point(
            double x,
            double y,
            boolean neg_y,
            boolean swap_xy
    ) {
        if (neg_y) y = -y;
        return swap_xy ? new Point(y, x) : new Point(x, y);
    }

    // specify location that the player will alternate between
    public void init(
            int id,
            int side,
            long turns,
            Point[][] pipers,
            Point[] rats
    ) {
        this.id = id;
        this.side = side;
        int n_pipers = pipers[id].length;
        pos = new Point[n_pipers][5];
        random_pos = new Point[n_pipers];
        pos_index = new int[n_pipers];
        for (int p = 0; p != n_pipers; ++p) {
            // spread out at the door level
            double door = 0.0;
            if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
            // pick coordinate based on where the player is
            boolean neg_y = id == 2 || id == 3;
            boolean swap = id == 1 || id == 3;
            // first and third position is at the door
            pos[p][0] = pos[p][2] = point(door, side * 0.5, neg_y, swap);
            // second position is chosen randomly in the rat moving area
            pos[p][1] = null;
            // fourth and fifth positions are outside the rat moving area
            pos[p][3] = point(door * -6, side * 0.5 + 3, neg_y, swap);
            pos[p][4] = point(door * +6, side * 0.5 + 3, neg_y, swap);
            // start with first position
            pos_index[p] = 0;
        }
    }

    private void updateCellWeights(Point[][] pipers, boolean[][] pipers_played, Point[] rats) {
    	// for now ignore influence of other players
    	for(Point rat : rats) {
    		Cell cell = getCell(rat);
    		if(cell != null) cell.weight++;
    	}
    }
    
    void determinePiperDests(Point[][] pipers, boolean[][] pipers_played, Point[] rats) {
    	// We're ignoring other inputs for now, just considering the rats and the instance variable 'grid'
    	ArrayList<Cell> cells = new ArrayList<Cell>();
    	for(Cell[] row : grid)
    		for(Cell cell : row)
    			cells.add(cell);
    	cells.sort(null);
    	int n_rats = 0;
    	for(Cell cell : cells)
    		n_rats += cell.weight;
    	int n_pipers = pipers[id].length;
    	ArrayList<Integer> unassigned_pipers = new ArrayList<Integer>();
    	for(int i = 0; i < n_pipers; ++i) unassigned_pipers.add(i);
    	for(int i = 0; i < cells.size(); ++i) {
    		// Probably need to reweight/increase this artificially too
    		int n_pipers_to_i = n_pipers*cells.get(i).weight/n_rats;
    		if(n_pipers_to_i == 0 || unassigned_pipers.size() == 0 || cells.get(i).weight <= 1) break;
    		
    		// Calculate distances of unassigned pipers to the points
        	double[] distances = new double[unassigned_pipers.size()];
        	for(int j = 0; j < distances.length; ++j) {
        		distances[j] = distance(cells.get(i).center, pipers[id][unassigned_pipers.get(j)]);
        	}
        	double nth_smallest = quickSelect(distances, n_pipers_to_i);
			// Send pipers towards cell i
        	for(int j = 0; j < distances.length; ++j)
        		if(distances[j] <= nth_smallest) {
        			// I'm abstracting away many details by using the current structure the TA gave for
        			// go to door -> go to location (he used random) -> back to door -> thru door -> repeat
        			// Simply updating random_pos here.
        			Integer piper = unassigned_pipers.get(j);
        			random_pos[piper] = cells.get(i).center;
        			unassigned_pipers.remove((Integer) piper);
        		}
    	}
    	// Possible (likely) in the case of few rats/sparse map that we will have ONLY unassigned pipers
    	// I'm also expecting a small number of unassigned pipers dense maps.
    	if(unassigned_pipers.size() > 0) {
    		//TODO what to do with unassigned pipers, especially in sparse maps (like the example).
    		// I'm thinking find rats closest to base and send closest piper to it?
    	}
    }
    
    static double distance(Point a, Point b) {
    	double x_dif = a.x - b.x;
		double y_dif = a.y - b.y;
		return Math.sqrt(x_dif*x_dif + y_dif*y_dif);
    }
    
    private Cell getCell(Point rat) {
    	for(Cell[] row : grid) {
    		for(Cell cell : row) {
    			if(cell.topleft.x <= rat.x &&
    					cell.topleft.x+cell.width >= rat.x &&
    					cell.topleft.y <= rat.y &&
    					cell.topleft.y+cell.length >= rat.y)
    				return cell;
    		}
    	}
    	return null;
    }
    
    // return next locations on last argument
    public void play(
            Point[][] pipers,
            boolean[][] pipers_played,
            Point[] rats,
            Move[] moves
    ) {
    	updateCellWeights(pipers, pipers_played, rats);
    	determinePiperDests(pipers, pipers_played, rats);
    	
        for (int p = 0; p != pipers[id].length; ++p) {
            Point src = pipers[id][p];
            Point dst = pos[p][pos_index[p]];
            // if null then get random position
            if (dst == null) dst = random_pos[p];
            // if position is reached
            if (Math.abs(src.x - dst.x) < 0.000001 &&
                    Math.abs(src.y - dst.y) < 0.000001) {
                // discard random position
                if (dst == random_pos[p]) random_pos[p] = null;
                // get next position
                if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
                dst = pos[p][pos_index[p]];
                // generate a new position if random
                if (dst == null) {
                    double x = (gen.nextDouble() - 0.5) * side * 0.9;
                    double y = (gen.nextDouble() - 0.5) * side * 0.9;
                    random_pos[p] = dst = new Point(x, y);
                }
            }
            // get move towards position
            moves[p] = move(src, dst, pos_index[p] > 1);
        }
    }
    
    // Quickselect from internet
    public static double quickSelect(double[] input_arr, int k) {
    	if (input_arr == null || input_arr.length <= k)
    		throw new Error();
    	
    	// copy to new array
    	double[] arr = new double[input_arr.length];
    	for(int i = 0; i < arr.length; ++i)
    		arr[i] = input_arr[i];

    	int from = 0, to = arr.length - 1;

    	// if from == to we reached the kth element
    	while (from < to) {
    		int r = from, w = to;
    		double mid = arr[(r + w) / 2];

    		// stop if the reader and writer meets
    		while (r < w) {

    			if (arr[r] >= mid) { // put the large values at the end
    				double tmp = arr[w];
    				arr[w] = arr[r];
    				arr[r] = tmp;
    				w--;
    			} else { // the value is smaller than the pivot, skip
    				r++;
    			}
    		}

    		// if we stepped up (r++) we need to step one down
    		if (arr[r] > mid)
    			r--;

    		// the r pointer is on the end of the first k elements
    		if (k <= r) {
    			to = r;
    		} else {
    			from = r + 1;
    		}
    	}

    	return arr[k];
    }
}

class Cell implements Comparable<Cell> {
	Point topleft;
	Point center;
	int length, width;
	int weight;
	
	public int compareTo(Cell other) {
		return Integer.compare(weight, other.weight);
	}
}
