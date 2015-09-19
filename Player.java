package pppp.g1;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    private int[] pos_index;
    private Point[] random_pos;

    // Array of queues. Each queue belongs to a piper and contains the moves queued up for that piper.
    private Queue<PointWrapper>[] queuedMoves;

    // Divide the board into a grid of cells. Each cell in the grid is
    // evaluated as a potential destination for a piper.
    private Grid grid;
    private Point gate;

    // create move towards specified destination


    // specify location that the player will alternate between
    public void init(int id, int side, long turns, Point[][] pipers, Point[] rats) {
        this.id = id;
        this.side = side;
        int numPipers = pipers[id].length;
        this.queuedMoves = new LinkedList[numPipers];

        // Initialize the grid of cells
        int cellSize = 20; // side must be multiple of cellSize
        int slices = side/cellSize;
        this.grid = new Grid(side, slices);
    }

    void determinePiperDests(Point[][] pipers, boolean[][] pipers_played, Point[] rats) {
        // We're ignoring other inputs for now, just considering the
        // rats and the instance variable 'grid'

        // Sort cells by decreasing weights
        ArrayList<Cell> cells = new ArrayList<Cell>();
        for (Cell[] row : this.grid.grid) {
            Collections.addAll(cells, row);
        }
        cells.sort(null);
        int n_rats = 0;
        Iterator<Cell> cellIter = cells.iterator();
        
        // What we're going to do is only consider cells with over twice the
        // average weight that are not literally at our gate (this would
        // basically lock pipers into base)
        double avg_weight = rats.length/cells.size(); // expected number of rats per cell
        while(cellIter.hasNext()) {
        	Cell cell = cellIter.next();
            // Discard cells that don't have high weight or are close by
        	if(cell.weight <= 2*avg_weight || Utils.distance(cell.center, gate) < 20) {
        		cellIter.remove();
        		continue;
        	}
        	n_rats += cell.weight;
        }
        for (Cell cell : cells) {
            n_rats += cell.weight;
        }

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
                        Utils.distance(cell.center, pipers[id][j])
                        : Double.MAX_VALUE;
            }
            // Get the n closest pipers to the cell i.
            double nth_smallest = Utils.quickSelect(distances, n_pipers_to_i);
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
        		rat_dist_gate[i] = Utils.distance(rats[i], gate);
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
        	double nth_closest_rat = Utils.quickSelect(rat_dist_gate, Math.min(n_unassigned, rat_dist_gate.length));
        	for(int i = 0; i < rat_dist_gate.length; ++i)
        		if(rat_dist_gate[i] <= nth_closest_rat) {
        			Integer closest_piper = null;
        			double dist_closest = Double.MAX_VALUE;
        			// From all the unassigned pipers, send the closest one towards this rat
        			for(Integer piper : unassigned_pipers) {
        				if(Utils.distance(pipers[id][piper], rats[i]) <= dist_closest) {
        					dist_closest = Utils.distance(pipers[id][piper], rats[i]);
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
                        double dist = Utils.distance(pipers[id][piper], rat);
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

    /**
     * Number of rats within specified range from the piper.
     * @param piper
     * @param rats
     * @param range
     */
    private static int numRatsInRange(Point piper, Point[] rats, int range) {
    	int numRats = 0;
        for (Point rat : rats) {
            numRats += Utils.distance(piper, rat) <= range ? 1 : 0;
        }
    	return numRats;
    }

    /**
     * Sends piper to position in front of gate where it waits, then enters the gate and waits again for the rats
     * to enter.
     * @param piperNumber
     */
    private void sendRatsThroughGate(int piperNumber) {
        Point gate = NavUtils.getGetGateRelativeCoordinates(this.id, this.side);
        int DISTANCE_OUTSIDE_GATE = 1;
        int DISTANCE_INSIDE_GATE = 5;
        Point pointOutsideGate = NavUtils
                .transformRelativeToAbsoluteCoordinates(this.id, this.side, gate.x + DISTANCE_OUTSIDE_GATE, gate.y);
        Point pointInsideGate = NavUtils
                .transformRelativeToAbsoluteCoordinates(this.id, this.side, gate.x - DISTANCE_INSIDE_GATE, gate.y);
        queuedMoves[piperNumber].add(new PointWrapper(pointOutsideGate, true));
        queuedMoves[piperNumber].add(null);
        queuedMoves[piperNumber].add(new PointWrapper(pointInsideGate, true));
        queuedMoves[piperNumber].add(null);
    }

    // return next locations on last argument
    public void play(Point[][] pipers, boolean[][] pipers_played, Point[] rats, Move[] moves) {
        this.grid.updateCellWeights(pipers, pipers_played, rats);
        determinePiperDests(pipers, pipers_played, rats);

        Point[] ourPipers = pipers[id];
        int numPipers = ourPipers.length;

        for (int piperNum = 0; piperNum <= numPipers; piperNum++) {
            Point source = ourPipers[piperNum];
            PointWrapper destinationWrapper = queuedMoves[piperNum].poll();
            Point destination = destinationWrapper.point;
            boolean playOnRouteToDestination = destinationWrapper.play;

            // If destination is null then stay in same position
            if (destination == null) {
                destination = source;
            }

            if (NavUtils.isDestinationReached(source, destination)) {
                moves[piperNum] = NavUtils.creatMove(source, destination, playOnRouteToDestination);
            }

            /* @Sagar: Please make necessary changes
            int range = 10;
        	if(pos_index[p] == 2 && numRatsInRange(ourPipers[p], rats, 10) == 0) --pos_index[p];

            // Different epsilons for gate and rat, since we dont need to be too close in the case of rats
            // But we need high precision to ensure we get through the gate properly with the rats
            const double GATE_EPSILON = 0.000001;
            const double RAT_EPSILON = 2;
            // If position is reached, ie. distance between src and destination is within some epsilon
            if ((Math.abs(src.x - dst.x) < GATE_EPSILON &&
                    Math.abs(src.y - dst.y) < GATE_EPSILON) || 
                    (Utils.distance(src, dst) < RAT_EPSILON && moveNum == 1)) {
            }
            */
        }
    }
}