package test;

import main.BranchAndBound;
import main.ParamsVRP;
import main.Route;
import main.utils.LoggingUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class testAlgo {
    private static final Logger logger = Logger.getLogger(testAlgo.class.getSimpleName());

    public static void main(String[] args) throws IOException {
        BranchAndBound bp = new BranchAndBound();
        ParamsVRP instance = new ParamsVRP();
        instance.initParams("dataset/R211.TXT");
        ArrayList<Route> initRoutes = new ArrayList<>();
        ArrayList<Route> bestRoutes = new ArrayList<>();

        bp.node(instance, initRoutes, null, bestRoutes, 0);
        logger.info(LoggingUtil.generateSolutionLog(bestRoutes));

    }
}
