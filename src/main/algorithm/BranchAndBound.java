package main.algorithm;

import main.domain.Parameters;
import main.domain.Route;
import main.domain.TreeBB;
import main.constants.Status;
import main.utils.LoggingUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class BranchAndBound {
    private double lowerBound;
    private double upperBound;
    private final Logger logger = Logger.getLogger(BranchAndBound.class.getSimpleName());

    public BranchAndBound() {
        lowerBound = -1E10;
        upperBound = 1E10;
    }

    public void EdgesBasedOnBranching(Parameters userParam, TreeBB branching, boolean recur) {
        int i;
        if (branching.father != null) { // stop before root node
            if (branching.branchValue == 0) { // forbid this edge (in this direction)
                // associate a very large distance to this edge to make it unattractive
                userParam.distance[branching.branchFrom][branching.branchTo] = userParam.veryBigNumber;
            } else { // impose this edge (in this direction)
                // associate a very large and unattractive distance to all edges
                // starting from "branchFrom" excepted the one leading to "branchTo"
                // and excepted when we start from depot (several vehicles)
                if (branching.branchFrom != 0) {
                    for (i = 0; i < branching.branchTo; i++)
                        userParam.distance[branching.branchFrom][i] = userParam.veryBigNumber;
                    for (i++; i < userParam.customerNum + 2; i++)
                        userParam.distance[branching.branchFrom][i] = userParam.veryBigNumber;
                }
                // associate a very large and unattractive distance to all edges ending
                // at "branchTo" excepted the one starting from "branchFrom"
                // and excepted when the destination is the depot (several vehicles)
                if (branching.branchTo != userParam.customerNum + 1) {
                    for (i = 0; i < branching.branchFrom; i++)
                        userParam.distance[i][branching.branchTo] = userParam.veryBigNumber;
                    for (i++; i < userParam.customerNum + 2; i++)
                        userParam.distance[i][branching.branchTo] = userParam.veryBigNumber;
                }
                // forbid the edge in the opposite direction
                userParam.distance[branching.branchTo][branching.branchFrom] = userParam.veryBigNumber;
            }
            if (recur)
                EdgesBasedOnBranching(userParam, branching.father, true);
        }
    }

    /**
     * @param userParam all the parameters provided by the users (cities, roads...)
     * @param routes all (but we could decide to keep only a subset) the routes considered up to now (to initialize the Column generation process)
     * @param branching BB branching context information for the current node to process (branching edge var, branching value, branching from...)
     * @param bestRoutes best solution encountered
     * @param depth depth of this node in TreeBB
     */
    public boolean node(Parameters userParam, ArrayList<Route> routes, TreeBB branching, ArrayList<Route> bestRoutes, int depth) throws Exception {
        int bestEdge1, bestEdge2, prevcity, city, bestVal;
        double coef, bestObj, change, CGobj;
        boolean feasible;

            // check first that we need to solve this node. Not the case if we have already found a solution within the gap precision
        if ((upperBound - lowerBound) / upperBound < userParam.gap)
            return true;

        // init
        if (branching == null) {
            // first call - root node
            branching = new TreeBB(null, null, -1, -1, -1, true);
        }

        // display some local info
        if (branching.branchValue < 1)
            logger.info("Edge from " + branching.branchFrom + " to "
                    + branching.branchTo + ": forbid");
        else
            logger.info("Edge from " + branching.branchFrom + " to "
                    + branching.branchTo + ": set");
        logger.info(LoggingUtil.generateRuntimeStatusLog());

        // Compute a solution for this node using Column generation
        ColumnGenerate CG = new ColumnGenerate();

        CGobj = CG.computeColGen(userParam, routes);
        // feasible ? Does a solution exist?
        if ((CGobj > 2 * userParam.maxLength) || (CGobj < -1e-6)) {
            // can only be true when the routes in the solution include forbidden edges (can happen when the BB set branching values)
            logger.info(LoggingUtil.generateStatusLog(Status.RELAX_INFEASIBLE, lowerBound, upperBound, depth, CGobj, routes.size()));

            return true; // stop this branch
        }
        branching.lowestValue = CGobj;

        // update the global lower bound when required
        if ((branching.father != null) && (branching.father.son0 != null)
                && branching.father.topLevel) {
            // all nodes above and on the left have been processed=> we can compute a new lower bound
            lowerBound = Math.min(branching.lowestValue, branching.father.son0.lowestValue);
            branching.topLevel = true;
        } else if (branching.father == null) // root node
            lowerBound = CGobj;

        if (branching.lowestValue > upperBound) {
            logger.info(LoggingUtil.generateStatusLog(Status.CUT, lowerBound, upperBound, depth, CGobj, routes.size()));

            return true; // cut this useless branch
        } else {
            // check the (integer) feasibility. Otherwise, search for a branching variable
            feasible = true;
            bestEdge1 = -1;
            bestEdge2 = -1;
            bestObj = -1.0;
            bestVal = 0;

            // transform the path variable (of the CG model) into edges variables
            for (int i = 0; i < userParam.customerNum + 2; i++)
                java.util.Arrays.fill(userParam.edges[i], 0.0);
            for (Route r : routes) {
                if (r.getQ() > 1e-6) { // we consider only the routes in the current
                    // local solution
                    ArrayList<Integer> path = r.getPath(); // get back the sequence of cities (path for thisRoute)
                    prevcity = 0;
                    for (int i = 1; i < path.size(); i++) {
                        city = path.get(i);
                        userParam.edges[prevcity][city] += r.getQ(); // convert into edges
                        prevcity = city;
                    }
                }
            }

            // find a fractional edge
            for (int i = 0; i < userParam.customerNum + 2; i++) {
                for (int j = 0; j < userParam.customerNum + 2; j++) {
                    coef = userParam.edges[i][j];
                    if ((coef > 1e-6)
                            && ((coef < 0.9999999999) || (coef > 1.0000000001))) {
                        // this Route.java has a fractional coefficient in the solution => should we branch on this one?
                        feasible = false;
                        // what if we impose this Route.java in the solution? Q=1
                        // keep the ref of the edge which should lead to the largest change
                        change = Math.min(coef, Math.abs(1.0 - coef));
                        change *= routes.get(i).getCost();
                        if (change > bestObj) {
                            bestEdge1 = i;
                            bestEdge2 = j;
                            bestObj = change;
                            bestVal = (Math.abs(1.0 - coef) > coef) ? 0 : 1;
                        }
                    }
                }
            }

            if (feasible) {
                if (branching.lowestValue < upperBound) { // new incumbent feasible solution!
                    upperBound = branching.lowestValue;
                    bestRoutes.clear();
                    for (Route r : routes) {
                        if (r.getQ() > 1e-6) {
                            Route optimum = new Route();
                            optimum.setCost(r.getCost());
                            optimum.path = r.getPath();
                            optimum.setQ(r.getQ());
                            bestRoutes.add(optimum);
                        }
                    }
                    logger.info(LoggingUtil.generateStatusLog(Status.OPTIMAL, lowerBound, upperBound, depth, CGobj, routes.size()));

                } else {
                    logger.info(LoggingUtil.generateStatusLog(Status.FEASIBLE, lowerBound, upperBound, depth, CGobj, routes.size()));
                }
                return true;
            } else {
                logger.info(LoggingUtil.generateStatusLog(Status.INTEGER_INFEASIBLE, lowerBound, upperBound, depth, CGobj, routes.size()));
                // ///////////////////////////////////////////////////////////
                // branching (diving strategy)

                // first branch -> set edges[bestEdge1][bestEdge2]=0
                // record the branching information in a tree list
                TreeBB newNode1 = new TreeBB(branching, null, bestEdge1, bestEdge2, bestVal, -1E10);
                // first version was not with bestVal but with 0

                // branching on edges[bestEdge1][bestEdge2]=0
                EdgesBasedOnBranching(userParam, newNode1, false);

                // the initial lp for the CG contains all the routes of the previous solution less the routes containing this arc
                ArrayList<Route> nodeRoutes = filterRoutes(userParam, routes);

                boolean ok;
                ok = node(userParam, nodeRoutes, newNode1, bestRoutes, depth + 1);
                if (!ok) {
                    return false;
                }

                branching.son0 = newNode1;

                // second branch -> set edges[bestEdge1][bestEdge2]=1
                // record the branching information in a tree list
                TreeBB newNode2 = new TreeBB(branching, null, bestEdge1, bestEdge2, 1-bestVal, -1E10);

                // branching on edges[bestEdge1][bestEdge2]=1
                // second branching=>need to reinitialize the dist matrix
                for (int i = 0; i < userParam.customerNum + 2; i++)
                    System.arraycopy(userParam.distanceOriginal[i], 0, userParam.distance[i], 0,
                            userParam.customerNum + 2);
                EdgesBasedOnBranching(userParam, newNode2, true);
                // the initial lp for the CG contains all the routes of the previous solution less the routes incompatible with this arc
                ArrayList<Route> nodeRoutes2 = filterRoutes(userParam, routes);
                ok = node(userParam, nodeRoutes2, newNode2, bestRoutes, depth + 1);

                // update the lowest feasible value of this node
                branching.lowestValue = Math.min(newNode1.lowestValue, newNode2.lowestValue);

                return ok;

            }
        }
    }

    private static ArrayList<Route> filterRoutes(Parameters userParam, ArrayList<Route> routes) {
        ArrayList<Route> nodeRoutes = new ArrayList<>();
        for (Route r : routes) {
            ArrayList<Integer> path = r.getPath();
            boolean accept = true;
            if (path.size() > 3) { // we must keep trivial routes (Depot-City-Depot) in the set to ensure feasibility of the CG
                int prevcity = 0;
                for (int j = 1; accept && (j < path.size()); j++) {
                    int city = path.get(j);
                    if (userParam.distance[prevcity][city] >= userParam.veryBigNumber - 1E-6)
                        accept = false;
                    prevcity = city;
                }
            }
            if (accept)
                nodeRoutes.add(r);
        }
        return nodeRoutes;
    }

}
