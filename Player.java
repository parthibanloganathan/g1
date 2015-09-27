package pppp.g1;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    
    private PiperState[] piper_states;

    final int INFLUENCE_RADIUS = 10;
    final int INFLUENCE_MIN_DIS = 2;
    final int PLAY_RAD = INFLUENCE_RADIUS;
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

        piper_states = new PiperState[numPipers];
        for (int i = 0; i < piper_states.length; ++i)
            piper_states[i] = PiperState.AT_GATE;

        this.movesQueue = new ArrayList<ArrayList<PiperDest>>(numPipers);
        for (int i = 0; i < numPipers; ++i) {
            ArrayList<PiperDest> inner = new ArrayList<PiperDest>();
            inner.add(new PiperDest(this.gate, false));
            inner.add(new PiperDest(new Point(0, 0), false));  // Placeholder
            inner.add(new PiperDest(gate_out, true));
            inner.add(new PiperDest(gate_in, true));
            movesQueue.add(inner);
        }

        // Initialize the grid of cells
        this.grid = new Grid(side, GRID_CELLSIZE);

        ticks = 0;
    }

    void updateGoal(int piperNum, Point newGoal, boolean playMusic) {
        movesQueue.get(piperNum).set(1, new PiperDest(newGoal, playMusic));
    }

    void stayInBase(int piperNum) {
        PiperDest pw = new PiperDest(gate_in, true);
        for (int i = 0; i < movesQueue.get(piperNum).size(); ++i) {
            movesQueue.get(piperNum).set(i, pw);
        }
    }

    Queue<Cell> getImportantCells(Grid grid, Point[] rats) {
        Queue<Cell> cells = new PriorityQueue<Cell>();
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
            if (cell.weight <= 1.3 * avg_weight ||  // TODO: Empirically evaluate the coefficent
                    Utils.distance(cell.center, this.gate) < GRID_CELLSIZE) {
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
        while (!cells.isEmpty()) {
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

            // Find the distance of the nth closest cell
            double nth_closest = Utils.quickSelect(dists, pipers2cell);

            // Send pipers towards the cell
            for (int i = 0; i < pipers[id].length; ++i)
                if (dists[i] <= nth_closest && dists[i] != Double.MAX_VALUE) {
                    // Send piper to this cell, while not playing music
                    double dist = Utils.distance(cell.center, movesQueue.get(i).get(1).point);
                    int n_rats = numRatsInRange(pipers[id][i], rats, PLAY_RAD);
                    if (dist < 2 * GRID_CELLSIZE) {
                        updateGoal(i, cell.center, false);
                    }
                    idle_pipers.remove((Integer) i);

                    if (dists[i] > GRID_CELLSIZE && n_rats < 3) {
                        piper_states[i] = PiperState.TO_GOAL;
                    }
                }
        }
    }

    /**
     * Returns the number of opponents near a rat.
     * @param rat
     * @param pipers
     * @param our_id
     */
    public int numOpponentsNearRat(Point rat, Point[][] pipers, int our_id) {
        int num_opponents_near_rat = 0;
        for (int id = 0; id < pipers.length; id++) {
            if (id != our_id) {
                for (Point opponent_piper : pipers[id]) {
                    // If opponent is within 2 ticks from rat, count the opponent as close to the rat.
                    if (Utils.distance(opponent_piper, rat) < 2*INFLUENCE_RADIUS) {
                        num_opponents_near_rat++;
                    }
                }
            }
        }
        return num_opponents_near_rat;
    }

    /**
     * Assign pipers to collect closest rats. This is used once the heaviest weight cells have been picked and the
     * unassigned pipers need to collect rats.
     * @param unassigned_pipers
     * @param rats
     * @param pipers
     */
    void assignGoalByRatDistance(
            ArrayList<Integer> unassigned_pipers, Point[] rats,
            Point[][] pipers
    ) {
        int n_unassigned = unassigned_pipers.size();

        // Set rat priority
        double[] ratsPriority = new double[rats.length];
        for (int i = 0; i < ratsPriority.length; ++i) {
            ratsPriority[i] = evaluateRatPriority(rats[i], pipers, id);
        }

        // Ensure that there are at least as many rats as pipers
        // if not first only assign 1 piper to each rat first
        // Then we assign the rest of the pipers to the closest rat
        double nth_priority = Utils.quickSelect(
                ratsPriority, Math.min(n_unassigned, ratsPriority.length)
        );

        for (int i = 0; i < ratsPriority.length; ++i)
            if (ratsPriority[i] <= nth_priority) {
                // From all the unassigned pipers, send the closest ones
                // towards this rat. May send multiple pipers if the rat has opponents nearby.
                int num_pipers_needed = numOpponentsNearRat(rats[i], pipers, id) + 1;
                ArrayList<Integer> nClosestPipers =
                        getIdsOfOurNClosestPipers(num_pipers_needed, rats[i], pipers[id], unassigned_pipers);

                for (int closePiper : nClosestPipers) {
                    // Piper is now assigned, remove from unassigned list
                    unassigned_pipers.remove(closePiper);
                    // Send piper to goal
                    updateGoal(closePiper, rats[i], false);
                }

                if (unassigned_pipers.size() == 0) return;
            }

        // In case we have additional pipers to spare
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
                    if (Utils.distance(rat, this.gate) > INFLUENCE_RADIUS) {
                        double dist = Utils.distance(pipers[id][piper], rat);
                        if (dist < closest_rat_dist) {
                            closest_rat_dist = dist;
                            closest_rat_pos = rat;
                        }
                    }
                }
                // If all rats are within 10m of gate just sit inside gate
                // and play music
                updateGoal(piper, closest_rat_pos, false);
                iter.remove();
            }
        }
    }

    /**
     * Get n closest of available pipers to a specified point.
     * @param n
     * @param p
     * @param pipers
     * @param available_pipers
     */
    ArrayList<Integer> getIdsOfOurNClosestPipers(int n, Point p, Point[] pipers, ArrayList<Integer> available_pipers) {
        PriorityQueue<PointWeight> closestPipers = new PriorityQueue<PointWeight>();
        ArrayList<Integer> nClosestPipers = new ArrayList<Integer>();

        for (int i = 0; i < pipers.length; i++) {
            if (available_pipers.contains(i)) {
                Point piper = pipers[i];
                double dist = Utils.distance(p, piper);
                closestPipers.add(new PointWeight(i, piper, dist));
            }
        }

        while (n > 0) {
            nClosestPipers.add(closestPipers.poll().id);
            n--;
        }

        return nClosestPipers;
    }

    /**
     * Evaluates how much we want this rat depending on distance and number of opponents in its vicinity.
     * @param rat
     */
    double evaluateRatPriority(Point rat, Point[][] pipers, int id) {
        // The closer the priority is to 0, the more we want the rat.
        double priority;
        double dist = Utils.distance(rat, this.gate);
        // We don't want to prioritize rats close to gate too much since they are easy to catch
        if (dist <= INFLUENCE_RADIUS) {
            priority = 2*dist;  // TODO: Empirically evaluate the coefficent
        } else {
            priority = dist;
        }
        // The more opponents near the rat, the lower the priority.
        priority += numOpponentsNearRat(rat, pipers, id); // TODO: Empirically evaluate the coefficent
        return priority;
    }

    void ensureReturningPipersHaveRats(Point[] pipers, Point[] rats) {
        // See if pipers are going back without rats; if so correct them.
        for (int piper_id = 0; piper_id < pipers.length; ++piper_id) {
        	double radius = PLAY_RAD;
        	if(rats.length < pipers.length)
        		radius = INFLUENCE_MIN_DIS + 0.5;
            if (numRatsInRange(pipers[piper_id], rats, radius) == 0) {

                // Piper is outside in the field and returning with zero rats
                // then send it to look for more
                if (piper_states[piper_id] == PiperState.TO_GATE)
                    piper_states[piper_id] = PiperState.TO_GOAL;

                // Piper is going through gate with zero rats
                // then send it out of gate
                if (piper_states[piper_id] == PiperState.UNLOAD)
                    piper_states[piper_id] = PiperState.AT_GATE;
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

    // return next locations on last argument
    public void play(
            Point[][] pipers, boolean[][] pipers_played, Point[] rats,
            Move[] moves
    ) {
    	++ticks;
    	//if(ticks%30 == 0) System.out.println(grid);
        ensureReturningPipersHaveRats(pipers[id], rats);
        grid.updateCellWeights(pipers, pipers_played, rats, id);

        ArrayList<Integer> idle_pipers = new ArrayList<Integer>();
        // Consider the "active duty" pipers that are not currently in base
        // They are either moving towards rats or herding them back (in this
        // case they change tactics rarely)
        for (int i = 0; i < pipers[id].length; ++i) {
            if (piper_states[i] == PiperState.TO_GOAL || piper_states[i] == PiperState.TO_GATE) {
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
            PiperDest target = movesQueue.get(piper_id).get(piper_states[piper_id].ordinal());

            double EPSILON = GATE_EPSILON;
            if (piper_states[piper_id] == PiperState.TO_GOAL)
                EPSILON = RAT_EPSILON;

            if (Utils.isAtDest(src, target.point, EPSILON)) {
                // Progress the movement to the next step.
                piper_states[piper_id] = piper_states[piper_id].nextState();
                // If we're in base and more rats are still around, wait for them to get in!
                if (piper_states[piper_id] == PiperState.AT_GATE && numRatsInRange(src, rats, PLAY_RAD) > 0)
                	piper_states[piper_id] = PiperState.UNLOAD;
                // Assign next destination.
                target = movesQueue.get(piper_id).get(piper_states[piper_id].ordinal());
            }
            
            // If destination is null then stay in same position
            if (target.point == null) {
                target.point = src;
            }
            
            moves[piper_id] = Utils.creatMove(src, target.point, target.play);
        }
    }
}