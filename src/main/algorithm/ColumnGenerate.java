package main.algorithm;


import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ilog.concert.*;
import ilog.cplex.*;
import main.algorithm.ShortestPath.ShortestPathWithRC;
import main.domain.Parameters;
import main.domain.Route;

/**
 * Asymmetric VRP with Resources Constraints (Time Windows and Capacity)
 * Branch and Price algorithm (Branch and Bound + Column generation)
 * For educational purpose only!  No code optimization.  Just to understand the main basic steps of the B&P algorithm.
 * Pricing through Dynamic Programming of the Short Path Problem with Resources Constraints (ShortestPathWithRC)
 * Algorithm inspired by the book
 * Desrosiers, Desaulniers, Solomon, "Column Generation", Springer, 2005 (GERAD, 25th anniversary)
 * => Branch and bound (class BPACVRPTW)
 * => Column generation (class ColumnGenerate) : chapter 3
 * => Pricing ShortestPathWithRC (class ShortestPathWithRC): chapter 2
 * CPLEX code for the column generation inspired by the example "CutStock.java" provided in the examples directory of the IBM ILOG CPLEX distribution
 *
 * @author mschyns
 * M.Schyns@ulg.ac.be
 */
public class ColumnGenerate {

    private final Logger logger = Logger.getLogger(ColumnGenerate.class.getSimpleName());

    /**
     * Creation of a new class similar to an ArrayList for CPLEX unknowns
     */
    static class IloNumVarArray {
        int _num = 0;
        IloNumVar[] _array = new IloNumVar[32];

        void add(IloNumVar ivar) {
            if (_num >= _array.length) {
                IloNumVar[] array = new IloNumVar[2 * _array.length];
                System.arraycopy(_array, 0, array, 0, _num);
                _array = array;
            }
            _array[_num++] = ivar;
        }

        IloNumVar getElement(int i) {
            return _array[i];
        }

        int getSize() {
            return _num;
        }
    }

    public double computeColGen(Parameters userParam, ArrayList<Route> routes) throws IOException {
        int i, j, prevcity, city;
        double cost, obj;
        double[] pi;
        boolean oncemore;

        try {

            // ---------------------------------------------------------
            // construct the model for the Restricted Master Problem
            // ---------------------------------------------------------
            // warning: for clarity, we create a new cplex env each time we start a Column Generation
            // this class contains (nearly) everything about CG and could be used independently
            // However, since the final goal is to encompass it inside Branch and Bound (BB),
            // it would (probably) be better to create only once the CPlex env when we
            // initiate the BB and to work with the same (but adjusted) lp matrix each time
            IloCplex cplex = new IloCplex();

            IloObjective objfunc = cplex.addMinimize();

            // for each vertex/client, one constraint (chapter 3, 3.23 )
            IloRange[] lpmatrix = new IloRange[userParam.customerNum];
            for (i = 0; i < userParam.customerNum; i++)
                lpmatrix[i] = cplex.addRange(1.0, Double.MAX_VALUE);
            // for each constraint, right member >=1
            // lpmatrix[i] = cplex.addRange(1.0, 1.0);
            // or for each constraint, right member=1 ... what is the best?

            // Declaration of the variables
            IloNumVarArray y = new IloNumVarArray(); // y_p to define whether a path p is used

            // Populate the lp matrix and the objective function
            // first with the routes provided by the argument 'routes' of the function
            // (in the context of the Branch and Bound, it would be a pity to start
            // again the CG from scratch at each node of the BB!)
            // (we should reuse parts of the previous solution(s))
            for (Route r : routes) {
                int v;
                cost = 0.0;
                prevcity = 0;
                for (i = 1; i < r.getPath().size(); i++) {
                    city = r.getPath().get(i);
                    cost += userParam.distance[prevcity][city];
                    prevcity = city;
                }

                r.setCost(cost);
                IloColumn column = cplex.column(objfunc, r.getCost());
                // obj coefficient
                for (i = 1; i < r.getPath().size() - 1; i++) {
                    v = r.getPath().get(i) - 1;
                    column = column.and(cplex.column(lpmatrix[v], 1.0));
                    // coefficient of y_i in (3.23) => 0 for the other y_p
                }
                y.add(cplex.numVar(column, 0.0, Double.MAX_VALUE));
                // creation of the variable y_i
            }
            // complete the lp with basic Route.java to ensure feasibility
            if (routes.size() < userParam.customerNum) { // a priori true only the first time
                addTrivialRoutes(userParam, routes, cplex, objfunc, lpmatrix, y);
            }

            // cplex.exportModel("model.lp");

            // CPlex params
            cplex.setParam(IloCplex.IntParam.RootAlgorithm, IloCplex.Algorithm.Primal);
            cplex.setOut(null);

            // ---------------------------------------------------------
            // column generation process
            // ---------------------------------------------------------
            DecimalFormat df = new DecimalFormat("#0000.00");
            oncemore = true;
            double[] prevobj = new double[100];
            int previ = -1;
            while (oncemore) {

                oncemore = false;
                // ---------------------------------------------------------
                // solve the current RMP
                // ---------------------------------------------------------
                if (!cplex.solve()) {
                    logger.info("CG: relaxation infeasible!");
                    return 1E10;
                }
                prevobj[(++previ) % 100] = cplex.getObjValue();
                // store the 30 last obj values to check stability after wards

                // cplex.exportModel("model.lp");

                // ---------------------------------------------------------
                // solve the sub problem to find new columns (if any)
                // ---------------------------------------------------------
                // first define the new costs for the sub problem objective function (ShortestPathWithRC)
                pi = cplex.getDuals(lpmatrix);
                for (i = 1; i < userParam.customerNum + 1; i++)
                    for (j = 0; j < userParam.customerNum + 2; j++)
                        userParam.cost[i][j] = userParam.distance[i][j] - pi[i - 1];

                // start dynamic programming
                ShortestPathWithRC sp = new ShortestPathWithRC(userParam);

                // shortest paths with negative cost
                // if ((previ>100) &&
                // (prevobj[(previ-3)%100]-prevobj[previ%100]<0.0003*Math.abs((prevobj[(previ-99)%100]-prevobj[previ%100]))))
                // {
                // System.out.print("/");
                // complete=true; // it the convergence is too slow, start a "complete"
                // shortestpast
                // }
                List<Route> routesSPPRC = sp.findShortestPath(userParam.customerNum);

                // /////////////////////////////
                // parameter here
                if (routesSPPRC.size() > 0) {
                    for (Route r : routesSPPRC) {
                        ArrayList<Integer> rout = r.getPath();
                        prevcity = rout.get(1);
                        cost = userParam.distance[0][prevcity];
                        IloColumn column = cplex.column(lpmatrix[rout.get(1) - 1], 1.0);
                        for (i = 2; i < rout.size() - 1; i++) {
                            city = rout.get(i);
                            cost += userParam.distance[prevcity][city];
                            prevcity = city;
                            column = column.and(cplex.column(lpmatrix[rout.get(i) - 1], 1.0));
                            // coefficient of y_i in (3.23) => 0 for the other y_p
                        }
                        cost += userParam.distance[prevcity][userParam.customerNum + 1];
                        column = column.and(cplex.column(objfunc, cost));
                        y.add(cplex.numVar(column, 0.0, Double.MAX_VALUE, "P" + routes.size())); // creation of the variable y_i
                        r.setCost(cost);
                        routes.add(r);

                        oncemore = true;
                    }
                    logger.info("CG Iter " + previ + " Current cost: " + df.format(prevobj[previ % 100]) + " " + routes.size() + " routes");

                }
            }

            for (i = 0; i < y.getSize(); i++)
                routes.get(i).setQ(cplex.getValue(y.getElement(i)));
            obj = cplex.getObjValue(); //To be entirely safe, we should recompute the obj using the distBase matrix instead of the dist matrix

            cplex.end();
            return obj;
        } catch (IloException e) {
            logger.severe("Concert exception caught '" + e + "' caught");
        }
        return 1E10;
    }

    private static void addTrivialRoutes(Parameters userParam, ArrayList<Route> routes, IloCplex cplex, IloObjective objfunc, IloRange[] lpmatrix, IloNumVarArray y) throws IloException {
        for (int i = 0; i < userParam.customerNum; i++) {
            double cost = userParam.distance[0][i + 1] + userParam.distance[i + 1][userParam.customerNum + 1];
            IloColumn column = cplex.column(objfunc, cost); // obj coefficient
            column = column.and(cplex.column(lpmatrix[i], 1.0)); // coefficient of y_i in (3.23) => 0 for the other y_p
            y.add(cplex.numVar(column, 0.0, Double.MAX_VALUE)); // creation of the variable y_i
            Route newroute = new Route();
            newroute.addCity(0);
            newroute.addCity(i + 1);
            newroute.addCity(userParam.customerNum + 1);
            newroute.setCost(cost);
            routes.add(newroute);
        }
    }
}
