package pppp.g1;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    
    private int[] pos_state;
//    private Random gen = new Random();

    final int AT_GATE = 0;
    final int TO_GOAL = 1;
    final int TO_GATE = 2;
    final int UNLOAD = 3;
    
    final int SPARSE_THRESH = 50;

    final int PLAY_RAD = 10;
    final int GRID_CELLSIZE = 20;
    final double GATE_EPSILON = 0.00001;
    final double RAT_EPSILON = 2;
    final double FROM_GATE = 1;
    final double IN_GATE = 4;

    // Array of queues. Each queue belongs to a piper and contains the
    // moves queued up for that piper.
    private ArrayList<ArrayList<PiperDest>> movesQueue;

    // Divide the board into a grid of cells. Each cell in the grid is
    // evaluated as a potential destination for a piper.
    private Grid grid;
    private Point gate;
    private Point gate_in;
    
    private int ticks;

    // specify location that the player will alternate between
    public void init(
            int id, int side, long turns, Point[][] pipers, Point[] rats
    ) {
        this.id = id;
        this.side = side;
        int numPipers = pipers[id].length;
        boolean neg_y = id == 2 || id == 3;
        boolean swap = id == 1 || id == 3;

        this.gate = Utils.point(0.0, side * 0.5, neg_y, swap);
        this.gate_in = Utils.point(0.0, side * 0.5 + IN_GATE, neg_y, swap);
        Point gate_out = Utils.point(0.0, side * 0.5 - FROM_GATE, neg_y, swap);
        
        pos_state = new int[numPipers];
        for (int i = 0; i < pos_state.length; ++i)
            pos_state[i] = AT_GATE;

        this.movesQueue = new ArrayList<>(numPipers);
        for (int i = 0; i < numPipers; ++i) {
            ArrayList<PiperDest> inner = new ArrayList<>();
            inner.add(new PiperDest(this.gate, false, false));
            inner.add(new PiperDest(new Point(0, 0), false, true));  // Placeholder
            inner.add(new PiperDest(gate_out, true, false));
            inner.add(new PiperDest(gate_in, true, false));
            movesQueue.add(inner);
        }

        // Initialize the grid of cells
        this.grid = new Grid(side, GRID_CELLSIZE);
        
        ticks = 0;
    }

    void updateGoal(int piperNum, Point newGoal, boolean playMusic, boolean override) {
        if (movesQueue.get(piperNum).get(1).override) {
            movesQueue.get(piperNum).set(1, new PiperDest(newGoal, playMusic, override));
        }
    }

    void stayInBase(int piperNum) {
        PiperDest pw = new PiperDest(gate_in, true, true);
        for (int i = 0; i < movesQueue.get(piperNum).size(); ++i) {
            movesQueue.get(piperNum).set(i, pw);
        }
    }

    Queue<Cell> getImportantCells(Grid grid, Point[] rats) {
        Queue<Cell> cells = new PriorityQueue<>();
        for (Cell[] row : grid.grid) {
            Collections.addAll(cells, row);
        }
        Iterator<Cell> cellIter = cells.iterator();

        // What we're going to do is only consider cells with over twice the
        // average weight that are not literally at our gate (this would
        // basically lock pipers into base).

        // expected number of rats per cell
        double avg_weight = rats.length / cells.size();
        while (cellIter.hasNext()) {
            Cell cell = cellIter.next();
            // Discard cells that don't have high weight or are close by
            if (cell.weight <= 1.3 * avg_weight ||
                    Utils.distance(cell.center, this.gate) < 20) {
                cellIter.remove();
            }
        }
        return cells;
    }

    /**
     * Assigning pipers to go to a particular cell based on cell weights.
     *
     * @param idle_pipers Pipers for which a goal can be assigned.
     * @param rats              The position of all the rats.
     * @param pipers            The positions of all the pipers in the game.
     */
    void assignGoalByCellWeights(
            ArrayList<Integer> idle_pipers, Point[] rats, Point[][] pipers
    ) {
        Queue<Cell> cells = getImportantCells(grid, rats);
        int sum_weights = 0;
        for (Cell cell : cells) {
            sum_weights += cell.weight;
        }

        int n_idle = idle_pipers.size();

        // Cicles through the highest rated cell first and downwards.
        while (cells.size() != 0) {
        	Cell cell = cells.poll();
            if (sum_weights == 0 || idle_pipers.size() == 0 || cell.weight <= 1)
                break;

            // Probably need to reweight/increase this artificially too
            // Temporarily changing the formula to only consider cells with
            // atleast twice average weight seems to have fixed this
            int pipers2cell = n_idle * (cell.weight / sum_weights);
            if (pipers2cell == 0)
                break;

            double[] dists = new double[pipers[id].length];
            for (int piper_id = 0; piper_id < pipers[id].length; ++piper_id) {
                dists[piper_id] = Utils.distance(
                        cell.center, pipers[id][piper_id]
                );

                // If the piper is busy/assigned, set dist to MAX
                if (!idle_pipers.contains(piper_id))
                    dists[piper_id] = Double.MAX_VALUE;
            }

            // Get the n closest pipers to the cell i.
            double nth_smallest = Utils.quickSelect(dists, pipers2cell);
            // Send pipers towards cell i
            for (int i = 0; i < pipers[id].length; ++i)
                if (dists[i] <= nth_smallest && dists[i] != Double.MAX_VALUE) {
                    // Send piper to this cell, while not playing music
                    int n_rats = numRatsInRange(pipers[id][i], rats, PLAY_RAD);
                    updateGoal(i, cell.center, false, true);
                    idle_pipers.remove((Integer) i);

                    if (dists[i] > 20 && n_rats < 3) {
                        pos_state[i] = TO_GOAL;
                    }
                }
        }
    }

    void assignGoalByRatDistance(
            ArrayList<Integer> unassigned_pipers, Point[] rats,
            Point[][] pipers
    ) {
        int n_unassigned = unassigned_pipers.size();
        double[] ratsDistToGate = new double[rats.length];
        for (int i = 0; i < ratsDistToGate.length; ++i) {
            ratsDistToGate[i] = distanceFunction(rats[i]);
        }
        // Ensure that there are at least as many rats as pipers
        // if not first only assign 1 piper to each rat first
        // Then we assign the rest of the pipers to the closest rat
        double nth_lowest_weight = Utils.quickSelect(
                ratsDistToGate, Math.min(n_unassigned, ratsDistToGate.length)
        );
        for (int i = 0; i < ratsDistToGate.length; ++i)
            if (ratsDistToGate[i] <= nth_lowest_weight) {
                Integer closest_piper = null;
                double dist_closest = Double.MAX_VALUE;
                // From all the unassigned pipers, send the closest one
                // towards this rat
                for (Integer j : unassigned_pipers) {
                    double p2r_dist = Utils.distance(pipers[id][j], rats[i]);
                    if (p2r_dist <= dist_closest) {
                        dist_closest = Utils.distance(pipers[id][j], rats[i]);
                        closest_piper = j;
                    }
                }

                if (closest_piper == null) {
                    throw new Error();
                }

                // Piper is now assigned, remove from unassigned list
                unassigned_pipers.remove(closest_piper);
                updateGoal(closest_piper, rats[i], false, false);
                if (unassigned_pipers.size() == 0) return;
            }

        // In case we had more pipers than rats, send to closest rat
        if (unassigned_pipers.size() > 0) {
            if (rats.length == 0) return;
            Iterator<Integer> iter = unassigned_pipers.iterator();
            while (iter.hasNext()) {
                Integer piper = iter.next();
                Point closest_rat_pos = null;
                double closest_rat_dist = Double.MAX_VALUE;
                for (Point rat : rats) {
                    // We ignore rats less than 10m from the gate because
                    // this will cause conflicts
                    if (Utils.distance(rat, this.gate) > 10) {
                        double dist = Utils.distance(pipers[id][piper], rat);
                        if (dist < closest_rat_dist) {
                            closest_rat_dist = dist;
                            closest_rat_pos = rat;
                        }
                    }
                }
                // If all rats are within 10m of gate just sit inside gate
                // and play music
                updateGoal(piper, closest_rat_pos, false, false);
                iter.remove();
            }
        }
    }

    double distanceFunction(Point rat) {
        // We need to ignore any rats that are being brought in at the moment
        // Best performance seems to be obtained by going for rats that
        // are not TOO close (these are very hard for others to steal) and
        // not TOO far. We go for hotly contested ones at a reasonable distance
        double y = Utils.distance(rat, this.gate);
        // Also we want to ignore rats too close to gate
        if (y <= 5) {
        	y = Double.MAX_VALUE;
        } else if (y <= side / 2) {
            y = (side - y) / 2;
        }
        return y;
    }

    void ensureReturningPipersHaveRats(Point[] pipers, Point[] rats) {
        // See if pipers are going back without rats; if so correct them.
        for (int piper_id = 0; piper_id < pipers.length; ++piper_id) {
        	double radius = PLAY_RAD/2;
        	if(rats.length < pipers.length)
        		radius = 2.5;
            if (numRatsInRange(pipers[piper_id], rats, radius) == 0) {

                // Piper is outside in the field and returning with zero rats
                // then send it to look for more
                if (pos_state[piper_id] == TO_GATE)
                    pos_state[piper_id] = TO_GOAL;

                // Piper is going through gate with zero rats
                // then send it out of gate
                if (pos_state[piper_id] == UNLOAD)
                    pos_state[piper_id] = AT_GATE;
            }
        }
    }
    
    void ensureReturningPipersCarryingUniqueRats(Point[] pipers, Point[] rats) {
    	HashSet<Point> ratsLeft = new HashSet<Point>();
    	for(Point rat : rats)
    		ratsLeft.add(rat);
    	for(int piper_id = 0; piper_id < pipers.length; ++piper_id) {
    		if(pos_state[piper_id] == TO_GATE) {
    			if(removeRatsInRange(pipers[piper_id], ratsLeft, PLAY_RAD) == 0) {
    				pos_state[piper_id] = TO_GOAL;
    			}
    		}
    		if(pos_state[piper_id] == UNLOAD) {
    			if(removeRatsInRange(pipers[piper_id], ratsLeft, PLAY_RAD) == 0) {
    				pos_state[piper_id] = AT_GATE;
    			}
    		}
    	}
    }

    /**
     * Number of rats within specified range from the piper.
     *
     * @param piper The position of a piper.
     * @param rats  The positions of all the rats.
     * @param range The music play radius around the piper.
     */
    private static int numRatsInRange(Point piper, Point[] rats, double range) {
        int numRats = 0;
        for (Point rat : rats) {
            numRats += Utils.distance(piper, rat) <= range ? 1 : 0;
        }
        return numRats;
    }

    /**
     * Returns number of rats within specified range from the piper.
     * Also removes rats from the ArrayList
     *
     * @param piper The position of a piper.
     * @param rats  The positions of all the rats.
     * @param range The music play radius around the piper.
     */
    private static int removeRatsInRange(Point piper, HashSet<Point> rats, double range) {
        int numRats = 0;
        Iterator<Point> ratIter = rats.iterator();
        while (ratIter.hasNext()) {
        	Point rat = ratIter.next();
        	if(Utils.distance(piper, rat) <= range) {
        		++numRats;
        		ratIter.remove();
        	}
        }
        return numRats;
    }
    
    // return next locations on last argument
    public void play(
            Point[][] pipers, boolean[][] pipers_played, Point[] rats,
            Move[] moves
    ) {
    	++ticks;
    	//if(ticks%30 == 0) System.out.println(grid);
        ensureReturningPipersHaveRats(pipers[id], rats);
        if(rats.length > SPARSE_THRESH) ensureReturningPipersCarryingUniqueRats(pipers[id], rats);
        
        grid.updateCellWeights(pipers, pipers_played, rats);

        ArrayList<Integer> idle_pipers = new ArrayList<Integer>();
        // Consider the "active duty" pipers that are not currently in base
        // They are either moving towards rats or herding them back (in this
        // case they change tactics rarely)
        for (int i = 0; i < pipers[id].length; ++i) {
            if (pos_state[i] == TO_GOAL || pos_state[i] == TO_GATE) {
                idle_pipers.add(i);
            }
        }

        // We begin by assigning pipers based on cell weights first.
        assignGoalByCellWeights(idle_pipers, rats, pipers);

        // Possible (likely) in the case of few rats/sparse map that we
        // will have ONLY unassigned pipers. I'm also expecting a small
        // number of unassigned pipers dense maps.
        if (idle_pipers.size() > 0) {
            assignGoalByRatDistance(idle_pipers, rats, pipers);
        }

        for (int piper_id = 0; piper_id < pipers[id].length; piper_id++) {
            Point src = pipers[id][piper_id];
            PiperDest trg = movesQueue.get(piper_id).get(pos_state[piper_id]);
            
            double EPSILON = GATE_EPSILON;
            if (pos_state[piper_id] == TO_GOAL)
                EPSILON = RAT_EPSILON;

            if (trg.point == null) {
                trg.point = src;
            }
            
            if (Utils.isAtDest(src, trg.point, EPSILON)) {
                // Progress the movement to the next step.
                pos_state[piper_id] = (pos_state[piper_id] + 1) % 4;
                movesQueue.get(piper_id).get(1).override = true;
                // If we're in base and more rats are still around, wait for them to get in!
                if (pos_state[piper_id] == AT_GATE && numRatsInRange(src, rats, PLAY_RAD) > 0)
                	pos_state[piper_id] = UNLOAD;
                // Assign next destination.
                trg = movesQueue.get(piper_id).get(pos_state[piper_id]);
            }
            
            // If destination is null then stay in same position
            if (trg.point == null) {
                trg.point = src;
            }
            
            moves[piper_id] = Utils.creatMove(src, trg.point, trg.play);
        }
    }
}