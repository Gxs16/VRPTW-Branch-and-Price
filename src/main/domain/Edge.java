package main.domain;

public class Edge {
    public double change;
    public int from;
    public int to;
    public int branchingDirection;

    public Edge(double change, int from, int to, int branchingDirection) {
        this.change = change;
        this.from = from;
        this.to = to;
        this.branchingDirection = branchingDirection;
    }
}
