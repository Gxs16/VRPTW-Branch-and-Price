package main.algorithm;

import main.constants.NumericalConstants;
import main.constants.Status;
import main.domain.Edge;
import main.domain.Parameters;
import main.domain.Route;
import main.domain.TreeBB;
import main.utils.LoggingUtil;

import java.util.ArrayList;
import java.util.logging.Logger;

public class BranchAndBound {
    private double lowerBound;
    private double upperBound;
    private final Logger logger = Logger.getLogger(BranchAndBound.class.getSimpleName());

    public BranchAndBound() {
        lowerBound = -NumericalConstants.veryBigNumber;
        upperBound = NumericalConstants.veryBigNumber;
    }

    public void EdgesBasedOnBranching(TreeBB branching) {
        if (branching.father != null) { // stop before root node
            if (branching.branchValue == 0) { // forbid this edge (in this direction)
                // associate a very large distance to this edge to make it unattractive
                branching.distance[branching.branchFrom][branching.branchTo] = NumericalConstants.veryBigNumber;
            } else { // impose this edge (in this direction)
                // associate a very large and unattractive distance to all edges
                // starting from "branchFrom" excepted the one leading to "branchTo"
                // and excepted when we start from depot (several vehicles)
                if (branching.branchFrom != 0) {
                    for (int i = 0; i < branching.branchTo; i++)
                        branching.distance[branching.branchFrom][i] = NumericalConstants.veryBigNumber;
                    for (int i = branching.branchTo+1; i < branching.distance.length; i++)
                        branching.distance[branching.branchFrom][i] = NumericalConstants.veryBigNumber;
                }
                // associate a very large and unattractive distance to all edges ending
                // at "branchTo" excepted the one starting from "branchFrom"
                // and excepted when the destination is the depot (several vehicles)
                if (branching.branchTo != branching.distance.length-1) {
                    for (int i = 0; i < branching.branchFrom; i++)
                        branching.distance[i][branching.branchTo] = NumericalConstants.veryBigNumber;
                    for (int i = branching.branchFrom+1; i < branching.distance.length; i++)
                        branching.distance[i][branching.branchTo] = NumericalConstants.veryBigNumber;
                }
                // forbid the edge in the opposite direction
                branching.distance[branching.branchTo][branching.branchFrom] = NumericalConstants.veryBigNumber;
            }
        }
    }

    /**
     * @param userParam  all the parameters provided by the users (cities, roads...)
     * @param routes     all (but we could decide to keep only a subset) the routes considered up to now (to initialize the Column generation process)
     * @param branching  BB branching context information for the current node to process (branching edge var, branching value, branching from...)
     * @param bestRoutes best solution encountered
     * @param depth      depth of this node in TreeBB
     */
    public boolean node(Parameters userParam, ArrayList<Route> routes, TreeBB branching, ArrayList<Route> bestRoutes, int depth) {
        // check first that we need to solve this node. Not the case if we have already found a solution within the gap precision
        if ((this.upperBound - this.lowerBound) / this.upperBound < NumericalConstants.gap)
            return true;

        // init
        if (branching == null) {
            // first call - root node
            branching = new TreeBB(null, -1, -1, -1, true, userParam.distanceOriginal);
        }

        // display some local info
        if (branching.branchValue < 1)
            logger.info("Edge from " + branching.branchFrom + " to " + branching.branchTo + ": forbid");
        else
            logger.info("Edge from " + branching.branchFrom + " to " + branching.branchTo + ": set");
        logger.info(LoggingUtil.generateRuntimeStatusLog());

        double CGobj = ColumnGenerate.compute(branching.distance, userParam, routes);
        // feasible ? Does a solution exist?
        if ((CGobj > 2 * userParam.maxLength) || (CGobj < -1e-6)) {
            // can only be true when the routes in the solution include forbidden edges (can happen when the BB set branching values)
            logger.info(LoggingUtil.generateStatusLog(Status.RELAX_INFEASIBLE, this.lowerBound, this.upperBound, depth, CGobj, routes.size()));

            return true; // stop this branch
        }
        branching.lowestValue = CGobj;

        // update the global lower bound when required
        if (branching.father != null && branching.father.son0 != null && branching.father.topLevel) {
            // all nodes above and on the left have been processed=> we can compute a new lower bound
            this.lowerBound = Math.min(branching.lowestValue, branching.father.son0.lowestValue);
            branching.topLevel = true;
        } else if (branching.father == null) // root node
            this.lowerBound = CGobj;

        if (branching.lowestValue > this.upperBound) {
            logger.info(LoggingUtil.generateStatusLog(Status.CUT, this.lowerBound, this.upperBound, depth, CGobj, routes.size()));

            return true; // cut this useless branch
        }

        // check the (integer) feasibility. Otherwise, search for a branching variable

        // transform the path variable (of the CG model) into edges variables
        for (Route r : routes) {
            if (r.getQuantity() > NumericalConstants.integerTolerance) { // we consider only the routes in the current
                // local solution
                ArrayList<Integer> path = r.getPath(); // get back the sequence of cities (path for thisRoute)
                int prevcity = 0;
                for (int i = 1; i < path.size(); i++) {
                    int city = path.get(i);
                    branching.edges[prevcity][city] += r.getQuantity(); // convert into edges
                    prevcity = city;
                }
            }
        }

        Edge bestEdge = findBestFractionalEdge(userParam, routes, branching.edges);

        if (bestEdge == null) {
            if (branching.lowestValue < this.upperBound) { // new incumbent feasible solution!
                this.upperBound = branching.lowestValue;
                bestRoutes.clear();
                for (Route r : routes) {
                    if (r.getQuantity() > NumericalConstants.integerTolerance) {
                        Route optimum = new Route();
                        optimum.setCost(r.getCost());
                        optimum.path = r.getPath();
                        optimum.setQuantity(r.getQuantity());
                        bestRoutes.add(optimum);
                    }
                }
                logger.info(LoggingUtil.generateStatusLog(Status.OPTIMAL, this.lowerBound, this.upperBound, depth, CGobj, routes.size()));

            } else {
                logger.info(LoggingUtil.generateStatusLog(Status.FEASIBLE, this.lowerBound, this.upperBound, depth, CGobj, routes.size()));
            }
            return true;
        }
        logger.info(LoggingUtil.generateStatusLog(Status.INTEGER_INFEASIBLE, this.lowerBound, this.upperBound, depth, CGobj, routes.size()));
        // branching (diving strategy)

        // first branch -> set edges[bestEdge1][bestEdge2]=0
        // record the branching information in a tree list
        TreeBB newNode1 = new TreeBB(branching, bestEdge.from, bestEdge.to, bestEdge.branchingDirection, -NumericalConstants.veryBigNumber, branching.distance);
        // first version was not with bestVal but with 0

        // branching on edges[bestEdge1][bestEdge2]=0
        EdgesBasedOnBranching(newNode1);

        // the initial lp for the CG contains all the routes of the previous solution less the routes containing this arc
        ArrayList<Route> nodeRoutes = filterRoutes(newNode1, routes);

        boolean ok = node(userParam, nodeRoutes, newNode1, bestRoutes, depth + 1);
        if (!ok) {
            return false;
        }

        branching.son0 = newNode1;

        // second branch -> set edges[bestEdge1][bestEdge2]=1
        // record the branching information in a tree list
        TreeBB newNode2 = new TreeBB(branching, bestEdge.from, bestEdge.to, 1 - bestEdge.branchingDirection, -NumericalConstants.veryBigNumber, branching.distance);

        // branching on edges[bestEdge1][bestEdge2]=1
        // second branching=>need to reinitialize the dist matrix
        EdgesBasedOnBranching(newNode2);
        // the initial lp for the CG contains all the routes of the previous solution less the routes incompatible with this arc
        ArrayList<Route> nodeRoutes2 = filterRoutes(newNode2, routes);
        ok = node(userParam, nodeRoutes2, newNode2, bestRoutes, depth + 1);

        // update the lowest feasible value of this node
        branching.lowestValue = Math.min(newNode1.lowestValue, newNode2.lowestValue);

        return ok;
    }

    private static ArrayList<Route> filterRoutes(TreeBB branching, ArrayList<Route> routes) {
        ArrayList<Route> nodeRoutes = new ArrayList<>();
        for (Route r : routes) {
            ArrayList<Integer> path = r.getPath();
            boolean accept = true;
            if (path.size() > 3) { // we must keep trivial routes (Depot-City-Depot) in the set to ensure feasibility of the CG
                int prevcity = 0;
                for (int j = 1; accept && (j < path.size()); j++) {
                    int city = path.get(j);
                    if (branching.distance[prevcity][city] >= NumericalConstants.veryBigNumber - 1E-6)
                        accept = false;
                    prevcity = city;
                }
            }
            if (accept)
                nodeRoutes.add(r);
        }
        return nodeRoutes;
    }

    private static Edge findBestFractionalEdge(Parameters userParam, ArrayList<Route> routes, double[][] edges) {
        Edge res = null;
        double bestObj = -1.0;
        for (int i = 0; i < userParam.customerNum + 2; i++) {
            for (int j = 0; j < userParam.customerNum + 2; j++) {
                double coefficient = edges[i][j];
                if ((coefficient > NumericalConstants.integerTolerance) && (coefficient < 1.0-NumericalConstants.integerTolerance || coefficient > 1.0+NumericalConstants.integerTolerance)) {
                    // this Route.java has a fractional coefficient in the solution => should we branch on this one?
                    // what if we impose this Route.java in the solution? Q=1
                    // keep the ref of the edge which should lead to the largest change
                    double change = Math.min(coefficient, Math.abs(1.0 - coefficient));
                    change *= routes.get(i).getCost();
                    if (change > bestObj) {
                        res = new Edge(change, i, j, (Math.abs(1.0 - coefficient) > coefficient) ? 0 : 1);
                        bestObj = change;
                    }
                }
            }
        }
        return res;
    }

}
