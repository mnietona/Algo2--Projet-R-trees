package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Point;

import java.util.List;

/**
 * Classe abstraite représentant un arbre R-Tree
 */
public abstract class RectangleTree {
    protected int N;
    protected Node root;

    public RectangleTree(int N) {
        this.N = N;
        this.root = new Node();
    }

    /**
     * Insère un nouveau polygon dans l'arbre R-Tree
     * @param label Le label du polygon
     * @param feature Le polygon à insérer
     */
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

    /**
     * Recherche le meilleur nœud pour insérer un nouveau polygon
     * @param node Le nœud à partir duquel effectuer la recherche
     * @param polygon Le polygon à insérer
     * @return Le meilleur nœud pour insérer le nouveau polygon
     */

    protected Node chooseNode(Node node, SimpleFeature polygon) {
        Envelope polygonEnvelope = getPolygonEnvelope(polygon);

        while (!isLeaf(node)) {
            node = findBestSubNode(node, polygonEnvelope);
        }
        return node;
    }

    /**
     * Retourne l'enveloppe du polygon
     *
     */
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

    /**
     * Calcule le coût d'expansion de l'enveloppe en ajoutant le nouveau polygon
     * @param mbr L'enveloppe du nœud
     * @param nextMBR L'enveloppe du nouveau polygon
     * @return Le coût d'expansion
     */
    public double calculateExpansionCost(Envelope mbr, Envelope nextMBR) {
        // On calcule l'expansion de l'enveloppe en ajoutant le nouveau polygon
        Envelope expandedMBR = new Envelope(mbr);
        expandedMBR.expandToInclude(nextMBR);
        return expandedMBR.getArea() - mbr.getArea();
    }

    /**
     * Ajoute un nouveau polygon dans le nœud donné
     * @param node Le nœud dans lequel ajouter le nouveau polygon
     * @param label Le label du nouveau polygon
     * @param polygon Le nouveau polygon à ajouter
     * @return Le nouveau nœud créé si le nœud donné est plein, null sinon
     */
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

    /**
     * Sépare le nœud donné en deux nouveaux nœuds
     * @param node Le nœud à séparer
     * @return Le nouveau nœud créé
     */
    protected Node split(Node node) {

        // On choisit les deux sous-nœuds les plus éloignés en fonction de l'heuristique
        int[] seeds = pickSeeds(node.getSubNodes());

        Node[] newGroup = createNewGroup(node, seeds);
        Node newGroup1 = newGroup[0];
        Node newGroup2 = newGroup[1];

        boolean[] assigned = initArray(node, seeds);
        int remaining = node.getSubNodes().size() - 2;

        while (remaining > 0) { // Tant qu'il reste des sous-nœuds à assigner
            assignNextNode(node, newGroup1, newGroup2, assigned);
            remaining--;
        }
        node.setSubNodes(newGroup1.getSubNodes());
        return newGroup2;
    }

    /**
     * Cree deux nouveaux groupes et y ajoute les deux sous-nœuds les plus éloignés
     * @param node Le nœud à partir duquel effectuer la recherche
     * @param seeds Les deux sous-nœuds les plus éloignés
     * @return Les deux nouveaux groupes
     */
    private Node[] createNewGroup(Node node, int[] seeds) {
        Node newGroup1 = new Node();
        Node newGroup2 = new Node();
        newGroup1.addSubNode(node.getSubNodes().get(seeds[0]));
        newGroup2.addSubNode(node.getSubNodes().get(seeds[1]));
        return new Node[]{newGroup1, newGroup2};
    }

    /**
     * Initialise le tableau des sous-nœuds déjà assignés
     * @param node Le nœud à partir duquel effectuer la recherche
     * @param seeds Les deux sous-nœuds les plus éloignés
     * @return Le tableau des sous-nœuds déjà assignés
     */
    private boolean[] initArray(Node node, int[] seeds) {
        boolean[] assigned = new boolean[node.getSubNodes().size()];
        assigned[seeds[0]] = true;
        assigned[seeds[1]] = true;
        return assigned;
    }

    /**
     * Recherche le sous-nœud le plus éloigné des deux groupes
     * @param node Le nœud à partir duquel effectuer la recherche
     * @param newGroup1 Le premier groupe
     * @param newGroup2 Le deuxième groupe
     * @param assigned Le tableau des sous-nœuds déjà assignés
     */
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

    /**
     * Calcule le gaspillage en combinant les deux enveloppes
     * @param e1 L'enveloppe du premier groupe
     * @param e2 L'enveloppe du deuxième groupe
     * @return Le coût du gaspillage
     */
    public double calculateWaste(Envelope e1, Envelope e2) {
        Envelope combinedEnvelope = new Envelope(e1);
        combinedEnvelope.expandToInclude(e2);
        return combinedEnvelope.getArea() - e1.getArea() - e2.getArea();
    }

    /**
     * Selectionne le sous-nœud le plus éloigné en fonction de l'heuristique
     * @param subNodes La liste des sous-nœuds
     * @param mbr1 L'enveloppe du premier groupe
     * @param mbr2 L'enveloppe du deuxième groupe
     * @param assigned Le tableau des sous-nœuds déjà assignés
     * @return L'index du sous-nœud le plus éloigné
     */
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

    /**
     * Recherche un point dans l'arbre
     * @param point Le point à rechercher
     * @return Le sous-nœud contenant le point
     */
    public Leaf search(Point point) {
        return searchPoint(this.root, point);
    }

    /**
     * Recherche un point dans un nœud
     * @param node Le nœud à partir duquel effectuer la recherche
     * @param point Le point à rechercher
     * @return Le sous-nœud contenant le point
     */
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

    /**
     * Méthode qui permet de choisir les deux sous-nœuds qui vont être les premiers enfants du nœud courant.
     * @param subnodes Liste des sous-nœuds du nœud courant
     * @return Tableau contenant les index des deux sous-nœuds choisis
     */
    protected abstract int[] pickSeeds(List<Node> subnodes);

    /**
     * Méthode qui permet de calculer le coût de l'ajout d'un sous-nœud à un nœud.
     * @param mbr1 Sous-nœud 1
     * @param mbr2 Sous-nœud 2
     * @param currentMBR MBR du nœud courant
     * @return Coût de l'ajout du sous-nœud
     */
    protected abstract double calculateCost(Envelope mbr1, Envelope mbr2, Envelope currentMBR);

}
