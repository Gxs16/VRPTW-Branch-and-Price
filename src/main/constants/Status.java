package main.constants;

public enum Status {
    /** cut this useless branch*/
    CUT("CUT"),
    INCUMBENT("INCUMBENT"),
    FEASIBLE("FEASIBLE"),
    RELAX_INFEASIBLE("RELAX INFEASIBLE"),
    INTEGER_INFEASIBLE("INTEGER INFEASIBLE"),
    /** already found a solution within the gap precision */
    WITHIN_PRECISION("WITHIN_PRECISION");

    private final String name;

    Status(String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
