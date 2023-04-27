package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un nœud de l'arbre R-Tree.
 */
public class Node {
    protected Envelope mbr;
    protected List<Node> subNodes;
    protected Node parent;

    public Node() {
        this.mbr = new Envelope();
        this.subNodes = new ArrayList<>();
    }

    /**
     * Ajoute un nœud fils au nœud courant.
     * @param node le nœud à ajouter
     */
    public void addSubNode(Node node) {
        subNodes.add(node);
        mbr.expandToInclude(node.getMBR());
        node.setParent(this);
    }

    public Node getParent() {
        return this.parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    /**
     * Met à jour le MBR du nœud courant en fonction des MBR de ses nœuds fils.
     */
    public void updateMBR() {
        this.mbr = new Envelope();
        for (Node subNode : subNodes) {
            this.mbr.expandToInclude(subNode.getMBR());
        }
    }

    /**
     * Ajoute une feuille au nœud courant.
     * @param leaf la feuille à ajouter
     */
    public void addLeaf(Leaf leaf) {
        addSubNode(leaf);
    }

    public boolean isEmpty() {
        return subNodes.isEmpty();
    }

    /**
     * Étend le MBR du nœud courant pour inclure le MBR passé en paramètre.
     * @param envelope le MBR à inclure
     */
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
