package main.algorithm.ShortestPath;

/**
 * We use a labelling algorithm.
 * Labels are attached to each vertex to specify the state of the resources when we follow a corresponding feasible path ending at this vertex
 */
public class Label {
    /**
     * current vertex
     */
    public int city;
    /**
     * previous Label in the same path (i.e. previous vertex in the same path with the state of the resources)
     */
    public int indexPrevLabel;
    /**
     * first resource: cost (e.g. distance or strict travel time)
     */
    public double cost;
    /**
     * second resource: travel time along the path (including wait time and service time)
     */
    public float travelTime;
    /**
     * third resource: demand,i.e. total quantity delivered to the clients encountered on this path
     */
    public double demand;
    /**
     * is this Label dominated by another one? i.e. if dominated, forget this path.
     */
    public boolean dominated;
    public boolean[] vertexVisited;

    public Label(int city, int indexPrevLabel, double cost, float travelTime, double demand, boolean dominated, boolean[] vertexVisited) {
        this.city = city;
        this.indexPrevLabel = indexPrevLabel;
        this.cost = cost;
        this.travelTime = travelTime;
        this.demand = demand;
        this.dominated = dominated;
        this.vertexVisited = vertexVisited;
    }
}
