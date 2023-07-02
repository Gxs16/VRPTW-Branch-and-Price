package main.constants;

public enum Status {
    /** cut this useless branch*/
    CUT("CUT"),
    OPTIMAL("OPTIMAL"),
    FEASIBLE("FEASIBLE"),
    RELAX_INFEASIBLE("RELAX INFEASIBLE"),
    INTEGER_INFEASIBLE("INTEGER INFEASIBLE");

    private final String name;

    Status(String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
