package main.utils;

import main.domain.Route;
import main.domain.TreeBB;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class LoggingUtil {
    private static final DecimalFormat df = new DecimalFormat("#0.000");

    public static String generateStatusLog(TreeBB branching, double lowerBound, double upperBound, int routeNum) {
        double gap = ((upperBound - lowerBound) / upperBound);
        return String.format("Node: %s Depth: %s | %s | Lower bound: %s | Upper bound: %s | Gap: %s | Local CG cost: %s | %s routes",
                branching.index, branching.depth, branching.status.getName(), LoggingUtil.df.format(lowerBound), LoggingUtil.df.format(upperBound), LoggingUtil.df.format(gap), LoggingUtil.df.format(branching.object), routeNum);
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

    public static String generateIterationLog(int previ, double[] prevobj, int routeNum) {
        return "CG Iter " + previ + " Current cost: " + LoggingUtil.df.format(prevobj[previ % 100]) + " " + routeNum + " routes";
    }
}
