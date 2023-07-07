package main.utils;

import main.domain.Route;
import main.constants.Status;
import main.domain.TreeBB;

import java.util.ArrayList;

public class LoggingUtil {

    public static String generateStatusLog(TreeBB branching, double lowerBound, double upperBound, int routeNum) {
        return String.format("Node: %s Depth: %s | %s | Lower bound: %s | Upper bound: %s | Gap: %s | Local CG cost: %s | %s routes",
                branching.index, branching.depth, branching.status.getName(), lowerBound, upperBound, ((upperBound - lowerBound) / upperBound), branching.object, routeNum);
    }

    public static String generateRuntimeStatusLog() {
        int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        return "Java Memory=> Total:" + (runtime.totalMemory() / mb)
                + " Max:" + (runtime.maxMemory() / mb) + " Used:"
                + ((runtime.totalMemory() - runtime.freeMemory()) / mb) + " Free: "
                + runtime.freeMemory() / mb;
    }

    public static String generateSolutionLog(ArrayList<Route> bestRoutes) {
        StringBuilder result = new StringBuilder("Best Solution: \n");
        double optCost = 0;
        for (Route bestRoute : bestRoutes) {
            result.append(bestRoute.path.toString()).append("-");
            optCost += bestRoute.cost;
        }

        result.append("Best Cost: ").append(optCost);
        return result.toString();
    }

    public static String generateBranchLog(TreeBB branch) {
        if (branch.branchValue < 1)
            return "Edge from " + branch.branchFrom + " to " + branch.branchTo + ": forbid";
        else
            return "Edge from " + branch.branchFrom + " to " + branch.branchTo + ": set";
    }
}
