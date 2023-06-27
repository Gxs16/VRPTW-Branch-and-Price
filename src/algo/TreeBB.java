package algo;

/**
 * this is a linked tree list recording all the branching during Branch and Bound
 */
class TreeBB {
    /**
     * link to the node processed before branching
     */
    TreeBB father;
    /**
     * link to the son on the left of the tree (edge=0; first processed) => need it to compute the global lower bound
     */
    TreeBB son0;
    /**
     * we branch on edges between cities => city origin of the edge
     */
    int branchFrom;
    /**
     * we branch on edges between cities => city destination of the edge
     */
    int branchTo;
    /**
     * we branch on edges between cities => value of the branching (remove edge=0; set edge=1)
     */
    int branchValue;
    /**
     * lower bound on the solution if we start from this node (i.e. looking only down for this tree)
     */
    double lowestValue;
    /**
     * to compute the global lower bound, need to know if everything above has been considered
     */
    boolean topLevel;

    public TreeBB(TreeBB father, TreeBB son0, int branchFrom, int branchTo, int branchValue, boolean topLevel) {
        this.father = father;
        this.son0 = son0;
        this.branchFrom = branchFrom;
        this.branchTo = branchTo;
        this.branchValue = branchValue;
        this.topLevel = topLevel;
    }

    public TreeBB(TreeBB father, TreeBB son0, int branchFrom, int branchTo, int branchValue, double lowestValue) {
        this.father = father;
        this.son0 = son0;
        this.branchFrom = branchFrom;
        this.branchTo = branchTo;
        this.branchValue = branchValue;
        this.lowestValue = lowestValue;
    }
}
