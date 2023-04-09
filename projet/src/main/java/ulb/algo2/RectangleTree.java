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

    private Node getRoot() {
        return root;
    }

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
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        Envelope polygonEnvelope = geometry.getEnvelopeInternal();

        while (!node.getSubNodes().isEmpty() && !(node.getSubNodes().get(0) instanceof Leaf)) {
            double minExpansion = Double.MAX_VALUE;
            Node bestNode = null;

            for (Node subnode : node.getSubNodes()) {
                Envelope expandedEnvelope = new Envelope(subnode.getMBR());
                expandedEnvelope.expandToInclude(polygonEnvelope);
                double expansion = expandedEnvelope.getArea() - subnode.getMBR().getArea();

                if (expansion < minExpansion) {
                    minExpansion = expansion;
                    bestNode = subnode;
                } else if (expansion == minExpansion) {
                    if (subnode.getMBR().getArea() < bestNode.getMBR().getArea()) {
                        bestNode = subnode;
                    }
                }
            }
            node = bestNode;
        }
        return node;
    }



    protected Node addLeaf(Node node, String label, SimpleFeature polygon) {
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        Envelope polygonEnvelope = geometry.getEnvelopeInternal();

        if (node.getSubNodes().isEmpty() || node.getSubNodes().get(0) instanceof Leaf) {
            node.addSubNode(new Leaf(label, polygon));
        } else {
            Node selectedNode = chooseNode(node, polygon);
            Node newNode = addLeaf(selectedNode, label, polygon);
            if (newNode != null) {
                node.addSubNode(newNode);
            }
        }
        node.expandEnvelope(polygonEnvelope);

        if (node.getSubNodes().size() >= N) {
            return split(node);
        } else {
            return null;
        }
    }


    protected Node split(Node node) {
        // Sélectionnez deux nœuds "graines" en utilisant la méthode pickSeeds
        int[] seeds = pickSeeds(node.getSubNodes());
        // Créez deux nouveaux groupes vides pour les nœuds séparés
        Node newGroup1 = new Node();
        Node newGroup2 = new Node();

        // Récupérez les MBR initiaux pour chaque groupe de graines
        Envelope mbr1 = node.getSubNodes().get(seeds[0]).getMBR();
        Envelope mbr2 = node.getSubNodes().get(seeds[1]).getMBR();

        // Ajoutez les nœuds "graines" aux nouveaux groupes
        newGroup1.addSubNode(node.getSubNodes().get(seeds[0]));
        newGroup2.addSubNode(node.getSubNodes().get(seeds[1]));

        // Créez un tableau pour suivre les nœuds déjà attribués
        boolean[] assigned = new boolean[node.getSubNodes().size()];
        assigned[seeds[0]] = true;
        assigned[seeds[1]] = true;

        // Comptez le nombre de nœuds restants à attribuer
        int remaining = node.getSubNodes().size() - 2;

        // Tant qu'il reste des nœuds à attribuer
        while (remaining > 0) {
            // Utilisez la méthode pickNext pour choisir le nœud suivant à attribuer
            int next = pickNext(node.getSubNodes(), mbr1, mbr2, assigned);
            Envelope nextMBR = node.getSubNodes().get(next).getMBR();

            // Calculez le coût d'expansion pour inclure le nœud suivant dans les MBR de chaque groupe
            Envelope mbr1Expanded = new Envelope(mbr1);
            mbr1Expanded.expandToInclude(nextMBR);
            double cost1 = mbr1Expanded.getArea() - mbr1.getArea();

            Envelope mbr2Expanded = new Envelope(mbr2);
            mbr2Expanded.expandToInclude(nextMBR);
            double cost2 = mbr2Expanded.getArea() - mbr2.getArea();

            // Attribuez le nœud suivant au groupe avec le coût d'expansion le plus faible
            if (cost1 < cost2) {
                newGroup1.addSubNode(node.getSubNodes().get(next));
                mbr1.expandToInclude(nextMBR);
            } else {
                newGroup2.addSubNode(node.getSubNodes().get(next));
                mbr2.expandToInclude(nextMBR);
            }

            // Marquez le nœud suivant comme attribué et décrémentez le nombre de nœuds restants
            assigned[next] = true;
            remaining--;
        }

        // Remplacez les nœuds du nœud d'origine par les nœuds du premier groupe
        node.setSubNodes(newGroup1.getSubNodes());
        // Retournez le deuxième groupe comme nouveau nœud
        return newGroup2;
    }


    protected abstract int[] pickSeeds(List<Node> subnodes);

    private int pickNext(List<Node> subnodes, Envelope mbr1, Envelope mbr2, boolean[] assigned) {
        double maxPreference = Double.NEGATIVE_INFINITY;
        int nextIndex = -1;

        for (int i = 0; i < subnodes.size(); i++) {
            if (!assigned[i]) {
                Envelope e = subnodes.get(i).getMBR();
                Envelope mbr1Expanded = new Envelope(mbr1);
                mbr1Expanded.expandToInclude(e);
                double cost1 = mbr1Expanded.getArea() - mbr1.getArea();

                Envelope mbr2Expanded = new Envelope(mbr2);
                mbr2Expanded.expandToInclude(e);
                double cost2 = mbr2Expanded.getArea() - mbr2.getArea();

                double preference = Math.abs(cost1 - cost2);
                if (preference > maxPreference) {
                    maxPreference = preference;
                    nextIndex = i;
                }
            }
        }
        return nextIndex;
    }

    public Leaf search(Point point) {
        return searchHelper(this.getRoot(), point);
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

    public int getSize() {
        return sizeTree(root);
    }

    public int sizeTree(Node node) {
        if (node == null) {
            return 0;
        }

        if (node instanceof Leaf) {
            return 1;
        } else {
            int count = 0;
            for (Node subnode : node.getSubNodes()) {
                count += sizeTree(subnode);
            }
            return count;
        }
    }



}
