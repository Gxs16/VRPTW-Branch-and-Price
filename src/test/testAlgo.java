package test;

import main.DataProcess;
import main.algorithm.BranchAndBound;
import main.domain.Parameters;
import main.domain.Route;
import main.utils.LoggingUtil;

import java.util.ArrayList;
import java.util.logging.Logger;

public class testAlgo {
    private static final Logger logger = Logger.getLogger(testAlgo.class.getSimpleName());

    public static void main(String[] args) throws Exception {
        String filePath = "dataset/R211.TXT";
        logger.info("================================================");
        logger.info("Start solving " + filePath);
        long startTime = System.currentTimeMillis();
        BranchAndBound bp = new BranchAndBound();
        Parameters instance = DataProcess.initParams(filePath);
        ArrayList<Route> initRoutes = new ArrayList<>();
        ArrayList<Route> bestRoutes = new ArrayList<>();

        bp.node(instance, initRoutes, null, bestRoutes);
        logger.info(LoggingUtil.generateSolutionLog(bestRoutes));
        long endTime = System.currentTimeMillis();
        logger.info(String.format("Execution time: %s ms", (endTime-startTime)));
    }
}
