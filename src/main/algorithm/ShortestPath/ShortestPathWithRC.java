package main.algorithm.ShortestPath;

import main.constants.NumericalConstants;
import main.domain.Parameters;
import main.domain.Route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

// shortest path with resource constraints
// inspired by Irnish and Desaulniers, "SHORTEST PATH PROBLEMS WITH RESOURCE CONSTRAINTS"
// for educational demonstration only - (nearly) no code optimization
//
// four main lists will be used:
// Labels: array (ArraList) => one dimensional unbounded vector
//		 list of all Labels created along the feasible paths (i.e. paths satisfying the resource constraints)
//		
// U: sorted list (TreeSet) => one dimensional unbounded vector
//		sorted list containing the indices of the unprocessed Labels (paths that can be extended to obtain a longer feasible path)
//
// P: sorted list (TreeSet) => one dimensional unbounded vector
//		sorted list containing the indices of the processed Labels ending at the depot with a negative cost
//
// city2labels: matrix (array of ArrayList) => nbclients x unbounded
//		for each city, the list of (indices of the) Labels attached to this city/vertex
//		before processing a Label at vertex i, we compare pairwise all Labels at the same vertex to remove the dominated ones

public class ShortestPathWithRC {
    Parameters userParam;
    double[][] distance;
    double[][] cost;
    ArrayList<Label> labels;

    public ShortestPathWithRC(Parameters userParam, double[][] distance, double[][] cost) {
        this.userParam = userParam;
        this.distance = distance;
        this.cost = cost;
    }

    public List<Route> findShortestPath(int numRoute) {

        // array of Labels
        this.labels = new ArrayList<>(2 * this.userParam.customerNum); // initial size at least larger than nb clients
        // unprocessed Labels list => ordered TreeSet List (?optimal:  need to be sorted like this?)
        TreeSet<Integer> U = new TreeSet<>(new LabelComparator(this.userParam, this.labels));   // unprocessed Labels list

        // processed Labels list => ordered TreeSet List
        TreeSet<Integer> P = new TreeSet<>(new LabelComparator(this.userParam, this.labels));   // processed Labels list


        boolean[] cust = new boolean[userParam.customerNum + 2];

        Arrays.fill(cust, false);
        cust[0] = true;
        this.labels.add(new Label(0, -1, 0.0, 0, 0, false, cust));    // first Label: start from depot (client 0)
        U.add(0);

        // for each city, an array with the index of the corresponding Labels (for dominance)
        int[] checkDom = new int[userParam.customerNum + 2];
        ArrayList<Integer>[] city2labels = new ArrayList[userParam.customerNum + 2];
        for (int i = 0; i < userParam.customerNum + 2; i++) {
            city2labels[i] = new ArrayList<>();
            checkDom[i] = 0;  // index of the first Label in city2labels that needs to be checked for dominance (last Labels added)
        }
        city2labels[0].add(0);

        int nbsol = 0;
        int maxsol = 2 * numRoute;
        while ((U.size() > 0) && (nbsol < maxsol)) {
            // second term if we want to limit to the first solutions encountered to speed up the ShortestPathWithRC (perhaps not the BP)
            // remark: we'll keep only numRoute, but we compute double numRoute!  It makes a huge difference=>we'll keep the most negative ones
            // this is something to analyze further!  how many solutions to keep and which ones?
            // process one Label => get the index AND remove it from U
            Integer currentIndex = U.pollFirst();
            Label current = labels.get(currentIndex);

            // check for dominance
            ArrayList<Integer> cleaning = new ArrayList<>();
            for (int i = checkDom[current.city]; i < city2labels[current.city].size(); i++) {
                int labelIndex1 = city2labels[current.city].get(i);// check for dominance between the Labels added since the last time we came here with this city and all the other ones
                Label label1 = labels.get(labelIndex1);
                for (int j = 0; j < i; j++) {
                    int labelIndex2 = city2labels[current.city].get(j);
                    Label label2 = labels.get(labelIndex2);
                    if (!(label1.dominated || label2.dominated)) {
                        dominateLabel(U, cleaning, labelIndex2, label2, label1);
                        boolean status = dominateLabel(U, cleaning, labelIndex1, label1, label2);
                        if (status) break;
                    }
                }
            }

            for (Integer c : cleaning)
                city2labels[current.city].remove(c);   // a little confusing but ok since c is an Integer and not an int!

            checkDom[current.city] = city2labels[current.city].size();  // update checkDom: all Labels currently in city2labels were checked for dom.

            // expand REF
            if (!current.dominated) {
                if (current.city == userParam.customerNum + 1) { // shortest path candidate to the depot!
                    if (current.cost < -1e-7) {                // SP candidate for the column generation
                        P.add(currentIndex);
                        nbsol = 0;
                        for (Integer labi : P) {
                            Label s = labels.get(labi);
                            if (!s.dominated)
                                nbsol++;
                        }
                    }
                } else {  // if not the depot, we can consider extensions of the path
                    for (int i = 0; i < userParam.customerNum + 2; i++) {
                        if ((!current.vertexVisited[i]) && (this.distance[current.city][i] < NumericalConstants.veryBigNumber - 1e-6)) {  // don't go back to a vertex already visited or along a forbidden edge
                            // travelTime
                            float tt = (float) (current.travelTime + userParam.travelTime[current.city][i] + userParam.serviceTime.get(current.city));
                            if (tt < userParam.readyTime.get(i))
                                tt = userParam.readyTime.get(i);
                            // demand
                            double d = current.demand + userParam.demand.get(i);

                            // is feasible?
                            if ((tt <= userParam.dueTime.get(i)) && (d <= userParam.capacity)) {
                                int idx = labels.size();
                                boolean[] newcust = new boolean[userParam.customerNum + 2];
                                System.arraycopy(current.vertexVisited, 0, newcust, 0, userParam.customerNum + 2);
                                newcust[i] = true;
                                //speedup: third technique - Feillet 2004 as mentioned in Laporte's paper
                                for (int j = 1; j <= userParam.customerNum; j++)
                                    if (!newcust[j]) {
                                        float tt2 = (float) (tt + userParam.travelTime[i][j] + userParam.serviceTime.get(i));
                                        double d2 = d + userParam.demand.get(j);
                                        if ((tt2 > userParam.dueTime.get(j)) || (d2 > userParam.capacity))
                                            newcust[j] = true;  // this client should not be visited anymore
                                    }

                                labels.add(new Label(i, currentIndex, current.cost + this.cost[current.city][i], tt, d, false, newcust));    // first Label: start from depot (client 0)
                                if (!U.add(idx)) {
                                    // only happens if there exists already a Label at this vertex with the same cost, time and demand and visiting the same cities before
                                    // It can happen with some paths where the order of the cities is permuted
                                    labels.get(idx).dominated = true; // => we can forget this Label and keep only the other one
                                } else
                                    city2labels[i].add(idx);

                            }
                        }
                    }
                }
            }
        }

        // filtering: find the path from depot to the destination
        return extractRoutes(numRoute, P);
    }

    private boolean dominateLabel(TreeSet<Integer> U, ArrayList<Integer> cleaning, int labelIndex, Label labelInspected, Label labelTarget) {
        boolean pathdom = true;
        boolean dominated = false;
        for (int k = 1; pathdom && (k < userParam.customerNum + 2); k++)
            pathdom = (!labelTarget.vertexVisited[k] || labelInspected.vertexVisited[k]);

        if (pathdom && (labelTarget.cost <= labelInspected.cost) && (labelTarget.travelTime <= labelInspected.travelTime) && (labelTarget.demand <= labelInspected.demand)) {
            labels.get(labelIndex).dominated = true;
            U.remove(labelIndex);
            cleaning.add(labelIndex);
            dominated = true;
        }
        return dominated;

    }


    private ArrayList<Route> extractRoutes(int numRoute, TreeSet<Integer> P) {

        ArrayList<Route> routes = new ArrayList<>();
        Integer lab;
        int i = 0;
        while ((i < numRoute) && ((lab = P.pollFirst()) != null)) {
            Label s = labels.get(lab);
            if (!s.dominated) {
                if ((s.cost < -1e-4)) {
                    Route newroute = new Route();
                    newroute.setCost(s.cost);
                    newroute.addCity(s.city);
                    int path = s.indexPrevLabel;
                    while (path >= 0) {
                        newroute.addCity(labels.get(path).city);
                        path = labels.get(path).indexPrevLabel;
                    }
                    newroute.reversePath();
                    routes.add(newroute);
                    i++;
                }
            }

        }
        return routes;
    }
}
