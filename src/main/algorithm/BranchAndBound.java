package main.algorithm;

import main.constants.NumericalConstants;
import main.constants.Status;
import main.domain.Edge;
import main.domain.Parameters;
import main.domain.Route;
import main.domain.NodeBB;
import main.utils.LoggingUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BranchAndBound {
    private double lowerBound;
    private double upperBound;
    private final Map<Integer, Double> index2Obj = new HashMap<>();
    private final Logger logger = Logger.getLogger(BranchAndBound.class.getSimpleName());

    public BranchAndBound() {
        lowerBound = -NumericalConstants.veryBigNumber;
        upperBound = NumericalConstants.veryBigNumber;
    }

    public void boundBasedOnEdges(NodeBB node) {
        if (node.father != null) { // stop before root node
            if (node.branchValue == 0) { // forbid this edge (in this direction)
                // associate a very large distance to this edge to make it unattractive
                node.distance[node.branchFrom][node.branchTo] = NumericalConstants.veryBigNumber;
            } else { // impose this edge (in this direction)
                // associate a very large and unattractive distance to all edges
                // starting from "branchFrom" excepted the one leading to "branchTo"
                // and excepted when we start from depot (several vehicles)
                if (node.branchFrom != 0) {
                    for (int i = 0; i < node.branchTo; i++)
                        node.distance[node.branchFrom][i] = NumericalConstants.veryBigNumber;
                    for (int i = node.branchTo+1; i < node.distance.length; i++)
                        node.distance[node.branchFrom][i] = NumericalConstants.veryBigNumber;
                }
                // associate a very large and unattractive distance to all edges ending
                // at "branchTo" excepted the one starting from "branchFrom"
                // and excepted when the destination is the depot (several vehicles)
                if (node.branchTo != node.distance.length-1) {
                    for (int i = 0; i < node.branchFrom; i++)
                        node.distance[i][node.branchTo] = NumericalConstants.veryBigNumber;
                    for (int i = node.branchFrom+1; i < node.distance.length; i++)
                        node.distance[i][node.branchTo] = NumericalConstants.veryBigNumber;
                }
                // forbid the edge in the opposite direction
                node.distance[node.branchTo][node.branchFrom] = NumericalConstants.veryBigNumber;
            }
        }
    }

    /**
     * @param userParam  all the parameters provided by the users (cities, roads...)
     * @param routes     all (but we could decide to keep only a subset) the routes considered up to now (to initialize the Column generation process)
     * @param node  BB node context information for the current node to process (branch edge var, branch value, branch from...)
     * @param bestRoutes best solution encountered
     */
    public void branch(Parameters userParam, ArrayList<Route> routes, NodeBB node, ArrayList<Route> bestRoutes) {
        // check first that we need to solve this node. Not the case if we have already found a solution within the gap precision
        if ((this.upperBound - this.lowerBound) / this.upperBound < NumericalConstants.gap) {
            node.status = Status.WITHIN_PRECISION;
            logger.info(LoggingUtil.generateStatusLog(node, this.lowerBound, this.upperBound, routes.size()));
            return;
        }

        // display some local info
        logger.info(LoggingUtil.generateBranchLog(node));

        double CGobj = ColumnGenerate.compute(node.distance, userParam, routes);
        node.object = CGobj;
        index2Obj.put(node.index, CGobj);
        node.lowerBound = CGobj;

        // update the global lower bound when required
        if (node.father != null && (node.index == (int) Math.pow(2, node.depth+1) - 2)) {
            // all nodes above and on the left have been processed=> we can compute a new lower bound
            double lowerBound = node.object;
            int leftIndex = node.index - 1;
            int fatherIndex = (node.index - 1) / 2;

            while (leftIndex >= 0) {
                lowerBound = Math.min(lowerBound, this.index2Obj.getOrDefault(leftIndex, NumericalConstants.veryBigNumber));
                leftIndex = fatherIndex - 1;
                fatherIndex = (fatherIndex - 1) / 2;
            }
            this.lowerBound = lowerBound;
        } else if (node.father == null){// root node
            this.lowerBound = CGobj;
        }
        // feasible ? Does a solution exist?
        if ((CGobj > 2 * userParam.maxLength) || (CGobj < -1e-6)) {
            // can only be true when the routes in the solution include forbidden edges (can happen when the BB set branching values)

            node.status = Status.RELAX_INFEASIBLE;
            logger.info(LoggingUtil.generateStatusLog(node, this.lowerBound, this.upperBound, routes.size()));

            return; // stop this branch
        }
        if (node.object > this.upperBound) {
            node.status = Status.CUT;
            logger.info(LoggingUtil.generateStatusLog(node, this.lowerBound, this.upperBound, routes.size()));
            return; // cut this useless branch
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
                    node.edges[prevcity][city] += r.getQuantity(); // convert into edges
                    prevcity = city;
                }
            }
        }

        Edge bestEdge = findBestFractionalEdge(userParam, routes, node.edges);

        if (bestEdge == null) {
            if (node.object < this.upperBound) { // new incumbent feasible solution!
                this.upperBound = node.object;
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
                node.status = Status.INCUMBENT;
                logger.info(LoggingUtil.generateStatusLog(node, this.lowerBound, this.upperBound, routes.size()));
            } else {
                node.status = Status.FEASIBLE;
                logger.info(LoggingUtil.generateStatusLog(node, this.lowerBound, this.upperBound, routes.size()));
            }
            return;
        }
        node.status = Status.INTEGER_INFEASIBLE;
        logger.info(LoggingUtil.generateStatusLog(node, this.lowerBound, this.upperBound, routes.size()));        // node (diving strategy)

        // first branch -> set edges[bestEdge1][bestEdge2]=0
        // record the branching information in a tree list
        node.sonLeft = new NodeBB(node.index * 2 + 1, node, bestEdge.from, bestEdge.to, bestEdge.branchingDirection, -NumericalConstants.veryBigNumber, node.distance);
        // first version was not with bestVal but with 0
        // bound on edges[bestEdge1][bestEdge2]=0
        boundBasedOnEdges(node.sonLeft);
        // the initial lp for the CG contains all the routes of the previous solution less the routes containing this arc
        ArrayList<Route> nodeRoutes = filterRoutes(node.sonLeft, routes);
        branch(userParam, nodeRoutes, node.sonLeft, bestRoutes);

        // second branch -> set edges[bestEdge1][bestEdge2]=1
        // record the branching information in a tree list
        node.sonRight = new NodeBB(node.index * 2 + 2, node, bestEdge.from, bestEdge.to, 1 - bestEdge.branchingDirection, -NumericalConstants.veryBigNumber, node.distance);
        // node on edges[bestEdge1][bestEdge2]=1
        // second node=>need to reinitialize the dist matrix
        boundBasedOnEdges(node.sonRight);
        // the initial lp for the CG contains all the routes of the previous solution less the routes incompatible with this arc
        ArrayList<Route> nodeRoutes2 = filterRoutes(node.sonRight, routes);
        branch(userParam, nodeRoutes2, node.sonRight, bestRoutes);

        // update the lowest feasible value of this node
        node.lowerBound = Math.min(node.sonLeft.lowerBound, node.sonRight.lowerBound);

    }

    private static ArrayList<Route> filterRoutes(NodeBB branching, ArrayList<Route> routes) {
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
