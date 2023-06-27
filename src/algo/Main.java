package algo;

import java.io.IOException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws IOException {
        BranchAndBound bp = new BranchAndBound();
        ParamsVRP instance = new ParamsVRP();
        instance.initParams("dataset/R211.TXT");
        ArrayList<Route> initRoutes = new ArrayList<>();
        ArrayList<Route> bestRoutes = new ArrayList<>();

        bp.node(instance, initRoutes, null, bestRoutes, 0);
        double optCost = 0;
        System.out.println();
        System.out.println("solution >>>");
        for (Route bestRoute : bestRoutes) {
            System.out.println(bestRoute.path);
            optCost += bestRoute.cost;
        }

        System.out.println("\nbest Cost = "+optCost);
    }

}
