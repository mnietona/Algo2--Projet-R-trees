package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Point;

import java.util.List;

/**
 * Une classe abstraite pour représenter un arbre de rectangles (R-Tree).
 */
public abstract class RectangleTree {
    protected int N;
    protected Node root;

    /**
     * Construit un arbre de rectangles vide.
     *
     * @param N Le nombre maximum d'enfants par nœud dans l'arbre.
     */
    public RectangleTree(int N) {
        this.N = N;
        this.root = new Node();
    }

    /**
     * Insère un nouvel élément (étiqueté par un SimpleFeature) dans l'arbre de rectangles.
     *
     * @param label L'étiquette de l'élément à insérer.
     * @param feature Le SimpleFeature représentant l'élément à insérer.
     */
    public void insert(String label, SimpleFeature feature) {
        // Si l'arbre est vide, créez une nouvelle feuille et définissez-la comme racine
        if (root.isEmpty()) {
            Leaf newLeaf = new Leaf(label, feature);
            root.addLeaf(newLeaf);

        } else {
            // Sinon, trouvez le nœud approprié pour insérer la nouvelle feuille
            Node chosenNode = chooseNode(root, feature);

            // Ajoutez la nouvelle feuille au nœud choisi et mettez à jour le MBR
            Node newNode = addLeaf(chosenNode, label, feature);

            // Si un nouveau nœud a été créé, ajustez l'arbre
            adjustTree(chosenNode, newNode);
        }
    }

    private void adjustTree(Node node, Node newNode) {
        // Mettez à jour le MBR du nœud actuel
        node.updateMBR();

        // Si le nœud actuel a un parent, ajustez l'arbre récursivement
        if (node.getParent() != null) {
            adjustTree(node.getParent(), newNode);
        } else if (newNode != null) {
            // Si nous sommes à la racine et qu'un nouveau nœud a été créé, créez une nouvelle racine et ajoutez les nœuds existants
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

    private Node findBestSubNode(Node node, Envelope polygonEnvelope) {
        double minExpansion = Double.MAX_VALUE;
        Node bestNode = null;

        for (Node subnode : node.getSubNodes()) {
            double expansion = calculateExpansionCost(subnode.getMBR(), polygonEnvelope);

            if (expansion < minExpansion) {
                minExpansion = expansion;
                bestNode = subnode;
            } else if (expansion == minExpansion && subnode.getMBR().getArea() < bestNode.getMBR().getArea()) {
                bestNode = subnode;
            }
        }
        return bestNode;
    }

    public double calculateExpansionCost(Envelope mbr, Envelope nextMBR) {
        Envelope expandedMBR = new Envelope(mbr);
        expandedMBR.expandToInclude(nextMBR);
        return expandedMBR.getArea() - mbr.getArea();
    }

    protected Node addLeaf(Node node, String label, SimpleFeature polygon) {
        // Ajoutez la nouvelle feuille au nœud choisi et mettez à jour le MBR
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        Envelope polygonEnvelope = geometry.getEnvelopeInternal();

        // Ajoutez la nouvelle feuille au nœud choisi
        node.addSubNode(new Leaf(label, polygon));
        node.expandEnvelope(polygonEnvelope); // Mettez à jour le MBR du nœud actuel

        // Si le nœud choisi est plein, divisez-le
        if (node.getSubNodes().size() >= N) {
            return split(node);
        } else {
            return null;
        }
    }


    protected Node split(Node node) {
        // Trouvez les deux groupes de nœuds les plus éloignés
        int[] seeds = pickSeeds(node.getSubNodes());
        Node[] newGroups = createAndInitNewGroups(node, seeds);
        Node newGroup1 = newGroups[0];
        Node newGroup2 = newGroups[1];

        // Initialisez le tableau des nœuds assignés
        boolean[] assigned = initAssignedArray(node, seeds);
        int remaining = node.getSubNodes().size() - 2;

        // Tant qu'il reste des nœuds à assigner, assignez le nœud le plus éloigné à l'un des deux groupes
        while (remaining > 0) {
            assignNextNode(node, newGroup1, newGroup2, assigned);
            remaining--;
        }
        // Mettez à jour le MBR des nouveaux groupes
        node.setSubNodes(newGroup1.getSubNodes());
        return newGroup2;
    }

    private Node[] createAndInitNewGroups(Node node, int[] seeds) {
        // Créez deux nouveaux groupes et ajoutez les nœuds de départ à l'un des deux groupes
        Node newGroup1 = new Node();
        Node newGroup2 = new Node();
        newGroup1.addSubNode(node.getSubNodes().get(seeds[0]));
        newGroup2.addSubNode(node.getSubNodes().get(seeds[1]));
        return new Node[]{newGroup1, newGroup2};
    }

    private boolean[] initAssignedArray(Node node, int[] seeds) {
        // Initialisez le tableau des nœuds assignés
        boolean[] assigned = new boolean[node.getSubNodes().size()];
        assigned[seeds[0]] = true;
        assigned[seeds[1]] = true;
        return assigned;
    }

    private void assignNextNode(Node node, Node newGroup1, Node newGroup2, boolean[] assigned) {
        // Trouvez le nœud le plus éloigné de l'un des deux groupes et assignez-le à ce groupe
        Envelope mbr1 = newGroup1.getMBR();
        Envelope mbr2 = newGroup2.getMBR();

        int next = pickNext(node.getSubNodes(), mbr1, mbr2, assigned);
        Envelope nextMBR = node.getSubNodes().get(next).getMBR();

        // Calculez le coût d'expansion de l'ajout d'un nœud à un groupe
        double cost1 = calculateExpansionCost(mbr1, nextMBR);
        double cost2 = calculateExpansionCost(mbr2, nextMBR);

        // Ajoutez le nœud le plus éloigné à l'un des deux groupes
        if (cost1 < cost2) {
            newGroup1.addSubNode(node.getSubNodes().get(next));
            mbr1.expandToInclude(nextMBR);
        } else {
            newGroup2.addSubNode(node.getSubNodes().get(next));
            mbr2.expandToInclude(nextMBR);
        }
        assigned[next] = true;
    }

    public double calculateWaste(Envelope e1, Envelope e2) {
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
            double cost = calculateCost(mbr1, mbr2, currentMBR);

            if (cost > maxCost) {
                maxCost = cost;
                nextNodeIndex = i;
            }
        }

        return nextNodeIndex;
    }

    /**
     * Recherche un élément contenant un point donné dans l'arbre de rectangles.
     *
     * @param point Le point a rechéché.
     * @return La feuille contenant le point, ou null si aucune feuille ne contient le point.
     */
    public Leaf search(Point point) {
        return searchHelper(this.root, point);
    }

    private Leaf searchHelper(Node node, Point point) {
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
                    Leaf result = searchHelper(subnode, point);
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
