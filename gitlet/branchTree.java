package gitlet;

import java.io.Serializable;

public class branchTree implements Serializable {
    Node p;
    String name;

    public branchTree(String name, Node pointer) {
        this.name = name;
        this.p = pointer;
    }

    public Node getP() {
        return this.p;
    }

    public String getName() {
        return this.name;
    }
}
