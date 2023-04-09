package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import java.util.ArrayList;
import java.util.List;

public class Node {
    protected Envelope mbr;
    protected List<Node> subNodes;

    protected Node parent;

    public Node() {
        this.mbr = new Envelope();
        this.subNodes = new ArrayList<>();
    }
    public void addSubNode(Node node) {
        subNodes.add(node);
        mbr.expandToInclude(node.getMBR());
        node.setParent(this); // Ajoutez cette ligne pour définir le parent
    }


    public Node getParent() {
        return this.parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void updateMBR() {
        this.mbr = new Envelope();
        for (Node subNode : subNodes) {
            this.mbr.expandToInclude(subNode.getMBR());
        }
    }


    public void addLeaf(Leaf leaf) {
        addSubNode(leaf);
    }

    public boolean isEmpty() {
        return subNodes.isEmpty();
    }

    public void expandEnvelope(Envelope envelope) {
        this.mbr.expandToInclude(envelope);
    }

    public Envelope getMBR() {
        return this.mbr;
    }

    public List<Node> getSubNodes() {
        return this.subNodes;
    }

    public void setSubNodes(List<Node> newSubnodes) { this.subNodes = newSubnodes; }

}
