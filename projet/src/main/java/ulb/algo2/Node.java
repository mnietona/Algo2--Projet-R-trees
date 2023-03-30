package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import java.util.ArrayList;
import java.util.List;

public class Node {
    protected Envelope mbr;
    protected List<Node> subnodes;

    public Node() {
        this.mbr = new Envelope();
        this.subnodes = new ArrayList<>();
    }

    public void expandEnvelope(Envelope envelope) {
        this.mbr.expandToInclude(envelope);
    }

    public Envelope getMBR() {
        return this.mbr;
    }

    public void setMBR(Envelope newMbr) { this.mbr = newMbr; }

    public List<Node> getSubnodes() {
        return this.subnodes;
    }

    public void setSubnodes(List<Node> newSubnodes) { this.subnodes = newSubnodes; }

}
