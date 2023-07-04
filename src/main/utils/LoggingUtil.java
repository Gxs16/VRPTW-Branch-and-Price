package main.utils;

import main.domain.Route;
import main.constants.Status;

import java.util.ArrayList;

public class LoggingUtil {
    public static String generateStatusLog(Status status, double lowerBound, double upperBound, int depth, double columnGenCost, int routeNum) {
        return String.format("%s | Lower bound: %s | Upper bound: %s | Gap: %s | Branch&Bound Depth: %s | Local CG cost: %s | %s routes",
                status.getName(), lowerBound, upperBound, ((upperBound - lowerBound) / upperBound), depth, columnGenCost, routeNum);
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
}
