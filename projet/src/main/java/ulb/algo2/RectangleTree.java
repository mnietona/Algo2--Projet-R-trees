package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Point;

import java.util.List;


public abstract class RectangleTree {
    protected int N;
    protected Node root;


    public RectangleTree(int N) {
        this.N = N;
        this.root = new Node();
    }

    public void insert(String label, SimpleFeature feature) {
        if (root.isEmpty()) {
            Leaf newLeaf = new Leaf(label, feature);
            root.addLeaf(newLeaf);

        } else {
            Node chosenNode = chooseNode(root, feature);

            Node newNode = addLeaf(chosenNode, label, feature);

            adjustTree(chosenNode, newNode);
        }
    }

    /**
     * Met à jour l'arbre R-Tree après l'insertion d'un nouveau nœud
     *
     * @param node Le nœud à partir duquel effectuer la mise à jour
     * @param newNode Le nouveau nœud inséré
     */
    private void adjustTree(Node node, Node newNode) {
        node.updateMBR();

        if (node.getParent() != null) {
            adjustTree(node.getParent(), newNode);
        }
        else if (newNode != null) {
            Node newRoot = new Node();
            newRoot.addSubNode(node);
            newRoot.addSubNode(newNode);
            root = newRoot;
        }
    }


    protected Node chooseNode(Node node, SimpleFeature polygon) {
        Envelope polygonEnvelope = getPolygonEnvelope(polygon);

        while (!isLeaf(node)) {
            node = findBestSubNode(node, polygonEnvelope);
        }
        return node;
    }

    private Envelope getPolygonEnvelope(SimpleFeature polygon) {
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        return geometry.getEnvelopeInternal();
    }

    private boolean isLeaf(Node node) {
        return node.getSubNodes().isEmpty() || node.getSubNodes().get(0) instanceof Leaf;
    }

    /**
     * Recherche le meilleur sous-nœud à partir du nœud donné pour insérer un nouveau polygon
     *
     * @param node Le nœud à partir duquel effectuer la recherche
     * @param polygonEnvelope L'enveloppe du nouveau polygon à insérer
     * @return Le meilleur sous-nœud pour insérer le nouveau polygon
     */
    private Node findBestSubNode(Node node, Envelope polygonEnvelope) {
        double minExpansion = Double.MAX_VALUE;
        Node bestNode = null;


        for (Node subnode : node.getSubNodes()) {
            double expansion = calculateExpansionCost(subnode.getMBR(), polygonEnvelope);

            // Si l'expansion est plus petite, on met à jour le meilleur sous-nœud
            if (expansion < minExpansion) {
                minExpansion = expansion;
                bestNode = subnode;
            }
            // Si deux sous-nœuds ont la même expansion, on prend celui avec la plus petite surface
            else if (expansion == minExpansion && subnode.getMBR().getArea() < bestNode.getMBR().getArea()) {
                bestNode = subnode;
            }
        }
        return bestNode;
    }


    public double calculateExpansionCost(Envelope mbr, Envelope nextMBR) {
        // On calcule l'expansion de l'enveloppe en ajoutant le nouveau polygon
        Envelope expandedMBR = new Envelope(mbr);
        expandedMBR.expandToInclude(nextMBR);
        return expandedMBR.getArea() - mbr.getArea();
    }

    protected Node addLeaf(Node node, String label, SimpleFeature polygon) {
        node.addSubNode(new Leaf(label, polygon));

        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        Envelope polygonEnvelope = geometry.getEnvelopeInternal();
        node.expandEnvelope(polygonEnvelope);

        if (node.getSubNodes().size() >= N) {
            return split(node);
        } else {
            return null;
        }
    }

    protected Node split(Node node) {

        // On choisit les deux sous-nœuds les plus éloignés en fonction de l'heuristique
        int[] seeds = pickSeeds(node.getSubNodes());
        Node[] newGroup = createNewGroup(node, seeds);
        Node newGroup1 = newGroup[0];
        Node newGroup2 = newGroup[1];

        boolean[] assigned = initAssignedArray(node, seeds);
        int remaining = node.getSubNodes().size() - 2;

        while (remaining > 0) {
            assignNextNode(node, newGroup1, newGroup2, assigned);
            remaining--;
        }
        node.setSubNodes(newGroup1.getSubNodes());
        return newGroup2;
    }

    private Node[] createNewGroup(Node node, int[] seeds) {
        // On crée deux nouveaux groupes et on y ajoute les deux sous-nœuds les plus éloignés
        Node newGroup1 = new Node();
        Node newGroup2 = new Node();
        newGroup1.addSubNode(node.getSubNodes().get(seeds[0]));
        newGroup2.addSubNode(node.getSubNodes().get(seeds[1]));
        return new Node[]{newGroup1, newGroup2};
    }

    private boolean[] initAssignedArray(Node node, int[] seeds) {
        boolean[] assigned = new boolean[node.getSubNodes().size()];
        assigned[seeds[0]] = true;
        assigned[seeds[1]] = true;
        return assigned;
    }

    private void assignNextNode(Node node, Node newGroup1, Node newGroup2, boolean[] assigned) {
        Envelope mbr1 = newGroup1.getMBR();
        Envelope mbr2 = newGroup2.getMBR();

        int next = pickNext(node.getSubNodes(), mbr1, mbr2, assigned); // On choisit le sous-nœud le plus éloigné
        Envelope nextMBR = node.getSubNodes().get(next).getMBR();

        double cost1 = calculateExpansionCost(mbr1, nextMBR);
        double cost2 = calculateExpansionCost(mbr2, nextMBR);

        if (cost1 < cost2) { // Si le coût d'expansion est plus petit pour le premier groupe, on l'ajoute
            newGroup1.addSubNode(node.getSubNodes().get(next));
            mbr1.expandToInclude(nextMBR);
        } else { // Sinon, on l'ajoute au deuxième groupe
            newGroup2.addSubNode(node.getSubNodes().get(next));
            mbr2.expandToInclude(nextMBR);
        }
        assigned[next] = true;
    }

    public double calculateWaste(Envelope e1, Envelope e2) {
        // On calcule le gaspillage en combinant les deux enveloppes
        Envelope combinedEnvelope = new Envelope(e1);
        combinedEnvelope.expandToInclude(e2);
        return combinedEnvelope.getArea() - e1.getArea() - e2.getArea();
    }

    public int pickNext(List<Node> subNodes, Envelope mbr1, Envelope mbr2, boolean[] assigned) {
        int nextNodeIndex = -1;
        double maxCost = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < subNodes.size(); i++) {
            if (assigned[i]) continue;

            Envelope currentMBR = subNodes.get(i).getMBR();
            // on calcule le coût en fonction de l'heuristique
            double cost = calculateCost(mbr1, mbr2, currentMBR);

            if (cost > maxCost) { // Si le coût est plus grand, on met à jour le coût et l'index
                maxCost = cost;
                nextNodeIndex = i;
            }
        }

        return nextNodeIndex;
    }

    public Leaf search(Point point) {
        return searchPoint(this.root, point);
    }

    private Leaf searchPoint(Node node, Point point) {
        if (node == null) {
            return null;
        }
        if (node instanceof Leaf leaf) {
            if (leaf.getMBR().contains(point.getCoordinate()) &&
                    ((Geometry) leaf.getPolygon().getDefaultGeometry()).contains(point)) {
                return leaf;
            }
        } else {
            for (Node subnode : node.getSubNodes()) {
                if (subnode.getMBR().contains(point.getCoordinate())) {
                    Leaf result = searchPoint(subnode, point);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    protected abstract int[] pickSeeds(List<Node> subnodes);

    protected abstract double calculateCost(Envelope mbr1, Envelope mbr2, Envelope currentMBR);

}
