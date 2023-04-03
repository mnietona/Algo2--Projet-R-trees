package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import java.util.ArrayList;
import java.util.List;

public class Node {
    protected Envelope mbr;
    protected List<Node> subNodes;

    public Node() {
        this.mbr = new Envelope();
        this.subNodes = new ArrayList<>();
    }
    public void addSubNode(Node node) {
        subNodes.add(node);
        mbr.expandToInclude(node.getMBR());
    }

    public void addLeaf(Leaf leaf) {
        addSubNode(leaf);
    }

    public boolean isEmpty() {
        return subNodes.isEmpty();
    }

    public boolean shouldSplit(int maxSubNodes) {
        return subNodes.size() >= maxSubNodes;
    }

    public void expandEnvelope(Envelope envelope) {
        this.mbr.expandToInclude(envelope);
    }

    public Envelope getMBR() {
        return this.mbr;
    }

    public void setMBR(Envelope newMbr) { this.mbr = newMbr; }

    public List<Node> getSubNodes() {
        return this.subNodes;
    }

    public void setSubNodes(List<Node> newSubnodes) { this.subNodes = newSubnodes; }

}
