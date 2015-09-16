package pppp.g1;

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

    // Divide the board into a grid of cells. Each cell in the grid is
    // evaluated as a potential destination for a piper.
    private Cell[][] grid = null;
    private Point gate = null;

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
            int id, int side, long turns, Point[][] pipers, Point[] rats
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
            if (n_pipers != 1)
                door = p * 1.8 / (n_pipers - 1) - 0.9;
            // pick coordinate based on where the player is
            boolean neg_y = id == 2 || id == 3;
            boolean swap = id == 1 || id == 3;
            // first and third position is at the door
            gate = pos[p][0] = pos[p][2] = point(door, side * 0.5, neg_y, swap);
            // second position is chosen randomly in the rat moving area
            pos[p][1] = null;
            // fourth and fifth positions are outside the rat moving area
            pos[p][3] = point(door * -6, side * 0.5 + 3, neg_y, swap);
            pos[p][4] = point(door * +6, side * 0.5 + 3, neg_y, swap);
            // start with first position
            pos_index[p] = 0;
        }

        // Initialize the grid of cells
        this.grid = createGrid(side, side/20);
    }

    /**
     * Create a grid of square cells each of side length size.
     *
     * @param side
     * @param slices
     * @return grid of cells
     */
    private Cell[][] createGrid(int side, int slices) {
        // The board consists of size^2 number of square cells.
        float size = (float) side / (float) slices;
        float half = (side / 2);
        Cell[][] grid = new Cell[slices][slices];
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < slices; j++) {
                grid[i][j] = new Cell(
                        new Point(  // X, Y - bottom-left corner
                                (i * size) - half,
                                (j * size) - half
                        ),
                        new Point(  // X, Y - center
                                (i + 0.5) * size - half,
                                (j + 0.5) * size - half
                        ),
                        size, 0
                );
            }
        }
        return grid;
    }

    /**
     * Update the weights of all cells.
     *
     * @param pipers
     * @param pipers_played
     * @param rats
     */
    private void updateCellWeights(
            Point[][] pipers, boolean[][] pipers_played, Point[] rats
    ) {
        // for now ignore influence of other players
    	for(Cell[] row : grid)
    		for(Cell cell : row)
    			cell.weight = 0;
        for (Point rat : rats) {
            Cell cell = getCell(rat);
            if (cell != null) cell.weight++;
        }
    }

    void determinePiperDests(Point[][] pipers, boolean[][] pipers_played, Point[] rats) {
        // We're ignoring other inputs for now, just considering the
        // rats and the instance variable 'grid'
        ArrayList<Cell> cells = new ArrayList<Cell>();
        for (Cell[] row : grid)
            Collections.addAll(cells, row);
        cells.sort(null);
        int n_rats = 0;
        Iterator<Cell> cellIter = cells.iterator();
        
        // What we're going to do is only consider cells with over twice the
        // average weight that are not literally at our gate (this would
        // basically lock pipers into base)
        double avg_weight = rats.length/cells.size();
        while(cellIter.hasNext()) {
        	Cell cell = cellIter.next();
        	if(cell.weight <= 2*avg_weight || PPPPUtils.distance(cell.center, gate) < 20) {
        		cellIter.remove();
        		continue;
        	}
        	n_rats += cell.weight;
        }
        for (Cell cell : cells)
            n_rats += cell.weight;

        int n_pipers = pipers[id].length;
        ArrayList<Integer> unassigned_pipers = new ArrayList<Integer>();
        // Consider the "active duty" pipers that are not currently in base
        // They are either moving towards rats or herding them back (in this
        // case they change tactics rarely)
        for (int i = 0; i < n_pipers; ++i)
            if(pos_index[i] == 1 || pos_index[i] == 2)
                unassigned_pipers.add(i);

        for (Cell cell : cells) {
            if (n_rats == 0 || unassigned_pipers.size() == 0 || cell.weight <= 1)
                break;
            // Probably need to reweight/increase this artificially too
            // Temporarily changing the formula to only consider cells with
            // atleast twice average weight seems to have fixed this
            int n_pipers_to_i = n_pipers * cell.weight / n_rats;
            if (n_pipers_to_i == 0)
                break;

            double[] distances = new double[pipers[id].length];
            for (int j = 0; j < distances.length; ++j) {
                // If the piper j is busy/assigned, set dist to MAX
                distances[j] = unassigned_pipers.contains(j) ?
                        PPPPUtils.distance(cell.center, pipers[id][j])
                        : Double.MAX_VALUE;
            }
            // Get the n closest pipers to the cell i.
            double nth_smallest = PPPPUtils.quickSelect(distances, n_pipers_to_i);
            // Send pipers towards cell i
            for (int j = 0; j < distances.length; ++j)
                if (distances[j] <= nth_smallest && distances[j] != Double.MAX_VALUE) {
                    // I'm abstracting away many details by using the
                    // current structure the TA gave for go to door ->
                    // go to location (he used random) -> back to door
                    // -> thru door -> repeat Simply updating random_pos here.
                    Integer piper = j;
                    random_pos[piper] = cell.center;
                    unassigned_pipers.remove(piper);
                    if (distances[piper] > 20 && n_rats_near(pipers[id][piper], rats) < 3)
                        pos_index[piper] = 1;
                    distances[piper] = Double.MAX_VALUE;
                }
        }

        // Possible (likely) in the case of few rats/sparse map that we
        // will have ONLY unassigned pipers. I'm also expecting a small
        // number of unassigned pipers dense maps.
        if (unassigned_pipers.size() > 0) {
        	int n_unassigned = unassigned_pipers.size();
        	double[] rat_dist_gate = new double[rats.length];
        	for(int i = 0; i < rat_dist_gate.length; ++i) {
        		rat_dist_gate[i] = PPPPUtils.distance(rats[i], gate);
        		// We need to ignore any rats that are being brought in at the moment
        		// Best performance seems to be obtained by going for rats that
        		// are not TOO close (these are very hard for others to steal) and not TOO far
        		// We go for hotly contested ones at a reasonable distance

        		// In effect, rat_dist_gate acts as a "weighting" and this can definitely be refined
        		if(rat_dist_gate[i] <= side/2) rat_dist_gate[i] = (side - rat_dist_gate[i])/2;
        		
        	}
        	// Ensure that there are at least as many rats as pipers
        	// if not first only assign 1 piper to each rat first
        	// Then we assign the rest of the pipers to the closest rat
        	double nth_closest_rat = PPPPUtils.quickSelect(rat_dist_gate, Math.min(n_unassigned,rat_dist_gate.length));
        	for(int i = 0; i < rat_dist_gate.length; ++i)
        		if(rat_dist_gate[i] <= nth_closest_rat) {
        			Integer closest_piper = null;
        			double dist_closest = Double.MAX_VALUE;
        			// From all the unassigned pipers, send the closest one towards this rat
        			for(Integer piper : unassigned_pipers) {
        				if(PPPPUtils.distance(pipers[id][piper], rats[i]) <= dist_closest) {
        					dist_closest = PPPPUtils.distance(pipers[id][piper], rats[i]);
        					closest_piper = piper;
        				}
        			}
        			// Piper is now assigned, remove from unassigned list
					unassigned_pipers.remove(closest_piper);
					random_pos[closest_piper] = rats[i];
					if(unassigned_pipers.size() == 0) return;
        		}
        	// In case we had more pipers than rats, send to closest rat
        	// I think send to random rat might be better here?
        	if(unassigned_pipers.size() > 0) {
        		if(rats.length == 0) return;
        		Iterator<Integer> iter = unassigned_pipers.iterator();
        		while(iter.hasNext()) {
        			Integer piper = iter.next();
        			Point closest_rat_pos = null;
        			double closest_rat_dist = Double.MAX_VALUE;
                    for (Point rat : rats) {
                        double dist = PPPPUtils.distance(pipers[id][piper], rat);
                        if (dist < closest_rat_dist) {
                            closest_rat_dist = dist;
                            closest_rat_pos = rat;
                        }
                    }
        			random_pos[piper] = closest_rat_pos;
        			iter.remove();
        		}
        	}
        }
    }
    
    // Yields the number of rats within range
    static int n_rats_near(Point piper, Point[] rats) {
    	int n = 0;
        for (Point rat : rats)
            n += PPPPUtils.distance(piper, rat) <= 10 ? 1 : 0;
    	return n;
    }

    private Cell getCell(Point rat) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                double left = cell.corner.x;
                double bottom = cell.corner.y;
                double top = cell.corner.y + cell.size;
                double right = cell.corner.x + cell.size;

                if (rat.y >= bottom && rat.y < top && rat.x >= left &&
                        rat.x < right)
                    return cell;
            }
        }
        return null;
    }

    // return next locations on last argument
    public void play(
            Point[][] pipers, boolean[][] pipers_played, Point[] rats,
            Move[] moves
    ) {
        updateCellWeights(pipers, pipers_played, rats);
        determinePiperDests(pipers, pipers_played, rats);

        Point[] ourPipers = pipers[id];
        int numPipers = ourPipers.length;
        for (int p = 0; p != numPipers; ++p) {
        	if(pos_index[p] == 2 && n_rats_near(ourPipers[p], rats) == 0) --pos_index[p];
            Point src = ourPipers[p];
            Point[] piperMoveList = pos[p];
            int moveNum = pos_index[p];
            // Get destination from list of moves the piper should make.
            Point dst = piperMoveList[moveNum];

            // if null then get random position
            if (dst == null) {
                dst = random_pos[p];
            }
            
            // Different epsilons for gate and rat, since we dont need to be too close in the case of rats
            // But we need high precision to ensure we get through the gate properly with the rats
            double GATE_EPSILON = 0.000001;
            double RAT_EPSILON = 2;
            // if position is reached, ie. distance between src and destination is within some epsilon
            if ((Math.abs(src.x - dst.x) < GATE_EPSILON &&
                    Math.abs(src.y - dst.y) < GATE_EPSILON) || 
                    (PPPPUtils.distance(src,dst) < RAT_EPSILON && moveNum == 1)) {
                // get next position
                // If we reach end of the moves list, reset
                if (++pos_index[p] == piperMoveList.length) {
                    pos_index[p] = 0;
                }
                moveNum = pos_index[p];
                dst = piperMoveList[moveNum];
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
}