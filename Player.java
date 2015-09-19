package pppp.g1;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;

    private int[] pipers_current_order;
    private Random gen = new Random();
    
    final int LEAVE_GATE = 0;
    final int TO_GOAL = 1;
    final int RETURN_TO_OUTSIDE_GATE = 2;
    final int GO_THROUGH_GATE = 3;
    
    final int RANGE = 10;
    
    final double GATE_EPSILON = 0.000001;
    final double RAT_EPSILON = 2;

    // Array of queues. Each queue belongs to a piper and contains the moves queued up for that piper.
    private ArrayList<PointWrapper>[] queuedMoves;

    // Divide the board into a grid of cells. Each cell in the grid is
    // evaluated as a potential destination for a piper.
    private Grid grid;
    private Point gateAbsolute;
    private Point gateRelative;

    // create move towards specified destination


    // specify location that the player will alternate between
    public void init(int id, int side, long turns, Point[][] pipers, Point[] rats) {
        this.id = id;
        this.side = side;
        int numPipers = pipers[id].length;
        gateRelative = new Point(0,0);
        gateAbsolute = NavUtils.transformRelativeToAbsoluteCoordinates(id, side, 0, 0);
        
        pipers_current_order = new int[numPipers];
        for(int i = 0;  i < pipers_current_order.length; ++i)
        	pipers_current_order[i] = LEAVE_GATE;
        
        this.queuedMoves = new ArrayList[numPipers];
        for(int i = 0; i < queuedMoves.length; ++i) {
        	queuedMoves[i] = new ArrayList<PointWrapper>();
        	addGateExitDestination(i);
        	queuedMoves[i].add(new PointWrapper(new Point(0,0),false)); // placeHolder "goal"
        	addGateReturnDestinations(i);
        }

        // Initialize the grid of cells
        int cellSize = 20; // side must be multiple of cellSize
        int slices = side/cellSize;
        this.grid = new Grid(side, slices);
    }
    
    void updatePiperGoalPosition(int piperNum, Point newGoal, boolean playMusic) {
    	queuedMoves[piperNum].set(piperNum, new PointWrapper(newGoal, playMusic));
    }
    
    void updatePiperCurrentOrder(int piperNum, int newOrder) {
    	pipers_current_order[piperNum] = newOrder;
    }
    
    void incrementPiperOrder(int piperNum) {
    	pipers_current_order[piperNum] = (pipers_current_order[piperNum] + 1) % 4;
    }
    
    PointWrapper getPiperDestination(int piperNum) {
    	return queuedMoves[piperNum].get(pipers_current_order[piperNum]);
    }
    
    ArrayList<Cell> getImportantCells(Grid grid, Point[] rats) {
        ArrayList<Cell> cells = new ArrayList<Cell>();
        for (Cell[] row : this.grid.grid) {
            Collections.addAll(cells, row);
        }
        cells.sort(null);
        Iterator<Cell> cellIter = cells.iterator();
        
        // What we're going to do is only consider cells with over twice the
        // average weight that are not literally at our gate (this would
        // basically lock pipers into base)
        double avg_weight = rats.length/cells.size(); // expected number of rats per cell
        while(cellIter.hasNext()) {
        	Cell cell = cellIter.next();
            // Discard cells that don't have high weight or are close by
        	if(cell.weight <= 2*avg_weight || Utils.distance(cell.center, gateAbsolute) < 20) {
        		cellIter.remove();
        		continue;
        	}
        }
        return cells;
    }

    void assignPipersByCellweights(ArrayList<Integer> unassigned_pipers, Point[] rats, Point[][] pipers) {
    	ArrayList<Cell> cells = getImportantCells(grid, rats);
    	int sum_cellweights = 0;
        for (Cell cell : cells) {
            sum_cellweights += cell.weight;
        }
    	
        int num_unassigned = unassigned_pipers.size();
        for (Cell cell : cells) {
            if (sum_cellweights == 0 || unassigned_pipers.size() == 0 || cell.weight <= 1)
                break;
            // Probably need to reweight/increase this artificially too
            // Temporarily changing the formula to only consider cells with
            // atleast twice average weight seems to have fixed this
            int n_pipers_to_i = num_unassigned * cell.weight / sum_cellweights;
            if (n_pipers_to_i == 0)
                break;

            double[] distances = new double[pipers[id].length];
            for (int piperNum = 0; piperNum < distances.length; ++piperNum) {
            	distances[piperNum] = Utils.distance(cell.center, pipers[id][piperNum]);

            	// If the piper is busy/assigned, set dist to MAX
            	if(unassigned_pipers.contains((Integer) piperNum))
            		distances[piperNum] = Double.MAX_VALUE;
            }

            // Get the n closest pipers to the cell i.
            double nth_smallest = Utils.quickSelect(distances, n_pipers_to_i);
            // Send pipers towards cell i
            for (int piperNum = 0; piperNum < distances.length; ++piperNum)
                if (distances[piperNum] <= nth_smallest && distances[piperNum] != Double.MAX_VALUE) {

                    // Send piper to this cell, while not playing music (playMusic = false)
                    updatePiperGoalPosition(piperNum, cell.center, false);
                    
                    unassigned_pipers.remove((Integer) piperNum);
                    if (distances[piperNum] > 20 && numRatsInRange(pipers[id][piperNum], rats, RANGE) < 3)
                        updatePiperCurrentOrder(piperNum, TO_GOAL);
                    distances[piperNum] = Double.MAX_VALUE;
                }
        }
    }
    
    void assignPipersByRatDistanceFunction(ArrayList<Integer> unassigned_pipers, Point[] rats, Point[][] pipers) {
    	int n_unassigned = unassigned_pipers.size();
    	double[] rat_weights = new double[rats.length];
    	for(int i = 0; i < rat_weights.length; ++i) {
    		rat_weights[i] = distanceFunction(rats[i]);
    	}
    	// Ensure that there are at least as many rats as pipers
    	// if not first only assign 1 piper to each rat first
    	// Then we assign the rest of the pipers to the closest rat
    	double nth_lowest_weight = Utils.quickSelect(rat_weights, Math.min(n_unassigned, rat_weights.length));
    	for(int i = 0; i < rat_weights.length; ++i)
    		if(rat_weights[i] <= nth_lowest_weight) {
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
				updatePiperGoalPosition(closest_piper, rats[i], false);
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
                updatePiperGoalPosition(piper, closest_rat_pos, false);
    			iter.remove();
    		}
    	}
    }
    
    double distanceFunction(Point rat) {
		// We need to ignore any rats that are being brought in at the moment
		// Best performance seems to be obtained by going for rats that
		// are not TOO close (these are very hard for others to steal) and not TOO far
		// We go for hotly contested ones at a reasonable distance
    	double y = Utils.distance(rat, gateAbsolute);
    	if(y <= side/2) y = (side - y)/2;
    	return y;
    }
    
    void ensureReturningPipersHaveRats(Point[][] pipers, boolean[][] pipers_played, Point[] rats) {
    	Point[] ourPipers = pipers[id];
    	
    	// See if pipers are going back without rats; if so correct them.
    	for(int piperNum = 0; piperNum < ourPipers.length; ++piperNum) {
    		if(numRatsInRange(ourPipers[piperNum], rats, RANGE) == 0) {
        	
	        	// Piper is outside in the field and returning with zero rats
	        	// then send it to look for more
	        	if(pipers_current_order[piperNum] == RETURN_TO_OUTSIDE_GATE)
	        		updatePiperCurrentOrder(piperNum, TO_GOAL);
	        	
	        	// Piper is going through gate with zero rats
	        	// then send it out of gate
	        	if(pipers_current_order[piperNum] == GO_THROUGH_GATE)
	        		updatePiperCurrentOrder(piperNum, LEAVE_GATE);
    		}
    	}
    }
    
    void determinePiperDests(Point[][] pipers, boolean[][] pipers_played, Point[] rats) {
        // We're ignoring other inputs for now, just considering the
        // rats and the instance variable 'grid'

        int n_pipers = pipers[id].length;
        ArrayList<Integer> unassigned_pipers = new ArrayList<Integer>();
        // Consider the "active duty" pipers that are not currently in base
        // They are either moving towards rats or herding them back (in this
        // case they change tactics rarely)
        for (int i = 0; i < n_pipers; ++i)
            if(pipers_current_order[i] == TO_GOAL || pipers_current_order[i] == RETURN_TO_OUTSIDE_GATE)
                unassigned_pipers.add(i);
        assignPipersByCellweights(unassigned_pipers, rats, pipers);

        // Possible (likely) in the case of few rats/sparse map that we
        // will have ONLY unassigned pipers. I'm also expecting a small
        // number of unassigned pipers dense maps.
        if (unassigned_pipers.size() > 0) {
        	assignPipersByRatDistanceFunction(unassigned_pipers, rats, pipers);
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
    private void addGateExitDestination(int piperNumber) {
    	queuedMoves[piperNumber].add(new PointWrapper(gateAbsolute, false));
    }

    private void addGateReturnDestinations(int piperNumber) {
        int DISTANCE_OUTSIDE_GATE = 1;
        int DISTANCE_INSIDE_GATE = 5;
        Point pointOutsideGate = NavUtils
                .transformRelativeToAbsoluteCoordinates(this.id, this.side, gateRelative.x, gateRelative.y + DISTANCE_OUTSIDE_GATE);
        Point pointInsideGate = NavUtils
                .transformRelativeToAbsoluteCoordinates(this.id, this.side, gateRelative.x, gateRelative.y - DISTANCE_INSIDE_GATE);
        queuedMoves[piperNumber].add(new PointWrapper(pointOutsideGate, true));
        queuedMoves[piperNumber].add(new PointWrapper(pointInsideGate, true));
    }

    // return next locations on last argument
    public void play(Point[][] pipers, boolean[][] pipers_played, Point[] rats, Move[] moves) {
    	ensureReturningPipersHaveRats(pipers, pipers_played, rats);
        this.grid.updateCellWeights(pipers, pipers_played, rats);
        determinePiperDests(pipers, pipers_played, rats);

        Point[] ourPipers = pipers[id];
        int numPipers = ourPipers.length;

        for (int piperNum = 0; piperNum < numPipers; piperNum++) {
            Point source = ourPipers[piperNum];
            PointWrapper destinationWrapper = getPiperDestination(piperNum);
            Point destination = destinationWrapper.point;
            boolean playOnRouteToDestination = destinationWrapper.play;

            // If destination is null then stay in same position
            if (destination == null) {
                destination = source;
            }
            
            double EPSILON = GATE_EPSILON;            
            if(pipers_current_order[piperNum] == TO_GOAL)
            	EPSILON = RAT_EPSILON;

            if (NavUtils.isDestinationReached(source, destination, EPSILON)) {
            	incrementPiperOrder(piperNum);
            	destinationWrapper = getPiperDestination(piperNum);
            	destination = destinationWrapper.point;
            	playOnRouteToDestination = destinationWrapper.play;
            }
            
            moves[piperNum] = NavUtils.creatMove(source, destination, playOnRouteToDestination);
            
            /* @Sagar: Please make necessary changes
            int range = 10;

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