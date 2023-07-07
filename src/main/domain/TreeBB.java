package main.domain;

import main.constants.Status;

/**
 * this is a linked tree list recording all the branching during Branch and Bound
 */
public class TreeBB {
    public int index;
    public int depth;
    /**
     * link to the node processed before branching
     */
    public TreeBB father;
    /**
     * link to the son on the left of the tree (edge=0; first processed) => need it to compute the global lower bound
     */
    public TreeBB sonLeft;
    /**
     * link to the node on the right of the tree
     */
    public TreeBB sonRight;
    /**
     * we branch on edges between cities => city origin of the edge
     */
    public int branchFrom;
    /**
     * we branch on edges between cities => city destination of the edge
     */
    public int branchTo;
    /**
     * we branch on edges between cities => value of the branching (remove edge=0; set edge=1)
     */
    public int branchValue;
    /**
     * linear relax object
     */
    public double object;
    /**
     * lower bound on the solution if we start from this node (i.e. looking only down for this tree)
     */
    public double lowerBound;
    /**
     * to compute the global lower bound, need to know if everything above has been considered
     */
    public boolean topLevel;
    /**
     * distances that will be updated during the B&B before being used in the CG & ShortestPathWithRC
     */
    public double[][] distance;
    /**
     * weight of each edge during branch and bound
     */
    public double[][] edges;
    public Status status;

    public TreeBB(double[][] distance) {
        this.father = null;
        this.branchFrom = -1;
        this.branchTo = -1;
        this.branchValue = -1;
        this.topLevel = true;

        this.distance = new double[distance.length][distance.length];
        for (int i = 0; i < distance.length; i++) {
            System.arraycopy(distance[i], 0, this.distance[i], 0,
                    distance[i].length);
        }
        this.edges = new double[distance.length][distance.length];
    }

    public TreeBB(int index, TreeBB father, int branchFrom, int branchTo, int branchValue, double object, double[][] distance) {
        this.index = index;
        this.father = father;
        this.depth = father.depth + 1;
        this.branchFrom = branchFrom;
        this.branchTo = branchTo;
        this.branchValue = branchValue;
        this.object = object;
        this.distance = new double[distance.length][distance.length];
        for (int i = 0; i < distance.length; i++) {
            System.arraycopy(distance[i], 0, this.distance[i], 0,
                    distance[i].length);
        }
        this.edges = new double[distance.length][distance.length];
    }
}
