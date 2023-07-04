package main.algorithm.ShortestPath;

import main.domain.Parameters;

import java.util.ArrayList;
import java.util.Comparator;

public class LabelComparator implements Comparator<Integer> {
    Parameters userParam;
    ArrayList<Label> labels;

    public LabelComparator(Parameters userParam, ArrayList<Label> labels) {
        this.userParam = userParam;
        this.labels = labels;
    }
    // the U treeSet is an ordered list
    // to maintain the order, we need to define a comparator: cost is the main criterium
    public int compare(Integer a, Integer b) {
        Label A = this.labels.get(a);
        Label B = this.labels.get(b);

        // Be careful!  When the comparator returns 0, it means that the two Labels are considered EXACTLY the same ones!
        // This comparator is not only used to sort the lists!  When adding to the list, a value of 0 => not added!!!!!
        if (A.cost-B.cost<-1e-7)
            return -1;
        else if (A.cost-B.cost>1e-7)
            return 1;
        else  {
            if (A.city==B.city) {
                if (A.travelTime -B.travelTime <-1e-7)
                    return -1;
                else if (A.travelTime -B.travelTime >1e-7)
                    return 1;
                else if (A.demand-B.demand<-1e-7)
                    return -1;
                else if (A.demand-B.demand>1e-7)
                    return 1;
                else {
                    int i=0;
                    while (i<userParam.customerNum +2) {
                        if (A.vertexVisited[i]!=B.vertexVisited[i]) {
                            if (A.vertexVisited[i])
                                return -1;
                            else
                                return 1;
                        }
                        i++;
                    }
                    return 0;
                }
            } else if (A.city>B.city)
                return 1;
            else
                return -1;
        }
    }
}
