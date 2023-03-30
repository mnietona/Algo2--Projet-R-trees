package ulb.algo2;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Envelope;

import java.util.List;


public class QuadraticRectangleTree extends RectangleTree {
    public QuadraticRectangleTree(int N) {
        super(N);
    }

    public void insert(String label, SimpleFeature polygon) {
        if (root == null) {
            root = new Leaf(label, polygon);
        } else {
            Envelope polygonEnvelope = ((Geometry) polygon.getDefaultGeometry()).getEnvelopeInternal();
            // Trouvez le nœud approprié pour insérer la nouvelle feuille
            Node chosenNode = chooseNode(root, polygonEnvelope);
            // Ajoutez la nouvelle feuille au nœud choisit
            Node newRoot = addLeaf(chosenNode, label, polygon);

            if (newRoot != null) {
                root = newRoot;
            }
        }
    }

    public Leaf search(double x, double y) {
        if (root == null) {
            return null;
        }

        Coordinate point = new Coordinate(x, y);
        return searchHelper(root, point);
    }

    private Leaf searchHelper(Node node, Coordinate point) {
        if (node instanceof Leaf) {
            Leaf leaf = (Leaf) node;
            Geometry polygon = (Geometry) leaf.getPolygon().getDefaultGeometry();
            Geometry pointGeometry = polygon.getFactory().createPoint(point);
            if (polygon.contains(pointGeometry)) {
                return leaf;
            }
        } else {
            for (Node child : node.getSubnodes()) {
                if (child.getMBR().contains(point)) {
                    Leaf found = searchHelper(child, point);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }



    protected Node chooseNode(Node node, Envelope polygonEnvelope) {
        // Si le nœud est une feuille, retournez-le
        if (node instanceof Leaf) {
            return node;
        }

        // Sinon, trouvez le sous-nœud dont l'expansion de l'enveloppe serait la plus petite
        Node bestNode = null;
        double smallestExpansion = Double.POSITIVE_INFINITY;

        for (Node subnode : node.getSubnodes()) {
            Envelope originalEnvelope = subnode.getMBR();
            Envelope expandedEnvelope = new Envelope(originalEnvelope);
            expandedEnvelope.expandToInclude(polygonEnvelope);

            double expansion = expandedEnvelope.getArea() - originalEnvelope.getArea();
            if (expansion < smallestExpansion) {
                smallestExpansion = expansion;
                bestNode = subnode;
            } else if (expansion == smallestExpansion) {
                // En cas d'égalité, choisissez le sous-nœud avec l'enveloppe la plus petite
                if (originalEnvelope.getArea() < bestNode.getMBR().getArea()) {
                    bestNode = subnode;
                }
            }
        }
        // Récursivement, continuez à chercher le meilleur nœud pour insérer le nouveau rectangle
        return chooseNode(bestNode, polygonEnvelope);
    }

    protected Node addLeaf(Node node, String label, SimpleFeature polygon) {
        // Créez une nouvelle feuille avec le label et le polygone donnés
        Leaf newLeaf = new Leaf(label, polygon);

        // Si le nœud est vide, ajoutez simplement la nouvelle feuille et mettez à jour l'enveloppe
        if (node.getSubnodes().isEmpty()) {
            node.getSubnodes().add(newLeaf);
            node.expandEnvelope(newLeaf.getMBR());
        } else {
            // Trouvez le meilleur nœud pour insérer la nouvelle feuille
            Node targetNode = chooseNode(node, newLeaf.getMBR());

            // Ajoutez la nouvelle feuille au nœud cible et mettez à jour l'enveloppe
            targetNode.getSubnodes().add(newLeaf);
            targetNode.expandEnvelope(newLeaf.getMBR());

            // Vérifiez si le nœud cible a trop de feuilles et doit être divisé
            if (targetNode.getSubnodes().size() > this.N) {
                Node newNode = split(targetNode);

                // Si le nœud cible est la racine, créez une nouvelle racine et ajoutez les nœuds divisés
                if (targetNode == this.root) {
                    Node newRoot = new Node();
                    newRoot.getSubnodes().add(targetNode);
                    newRoot.getSubnodes().add(newNode);
                    newRoot.expandEnvelope(targetNode.getMBR());
                    newRoot.expandEnvelope(newNode.getMBR());
                    this.root = newRoot;
                } else {
                    // Sinon, ajoutez le nouveau nœud divisé au parent et mettez à jour l'enveloppe
                    Node parentNode = findParentNode(this.root, targetNode);
                    parentNode.getSubnodes().add(newNode);
                    parentNode.expandEnvelope(newNode.getMBR());
                    // Vérifiez si le nœud parent a trop de sous-nœuds et doit être divisé
                    if (parentNode.getSubnodes().size() > this.N) {
                        split(parentNode);
                    }
                }
            }
        }
        return node;
    }

    private Node findParentNode(Node currentNode, Node targetNode) {
        if (currentNode instanceof Leaf) {
            return null;
        }

        for (Node subnode : currentNode.getSubnodes()) {
            if (subnode == targetNode) {
                return currentNode;
            }

            Node parentNode = findParentNode(subnode, targetNode);
            if (parentNode != null) {
                return parentNode;
            }
        }

        return null;
    }



    protected Node split(Node node) {
        // Trouver les paires de sous-nœuds les plus éloignées
        Node[] seedNodes = pickSeeds(node.getSubnodes());

        // Créer deux nouveaux nœuds et y insérer les graines
        Node newGroup1 = new Node();
        Node newGroup2 = new Node();
        newGroup1.getSubnodes().add(seedNodes[0]);
        newGroup2.getSubnodes().add(seedNodes[1]);
        newGroup1.expandEnvelope(seedNodes[0].getMBR());
        newGroup2.expandEnvelope(seedNodes[1].getMBR());

        // Répartir les sous-nœuds restants entre les deux groupes
        distribute(node.getSubnodes(), newGroup1, newGroup2);

        // Si le nœud divisé est la racine, créer une nouvelle racine et la renvoyer
        if (node == this.root) {
            Node newRoot = new Node();
            newRoot.getSubnodes().add(newGroup1);
            newRoot.getSubnodes().add(newGroup2);
            newRoot.expandEnvelope(newGroup1.getMBR());
            newRoot.expandEnvelope(newGroup2.getMBR());
            return newRoot;
        }

        // Sinon, mettre à jour le parent du nœud divisé pour inclure les deux nouveaux nœuds
        Node parentNode = findParentNode(this.root, node);
        parentNode.getSubnodes().remove(node);
        parentNode.getSubnodes().add(newGroup1);
        parentNode.getSubnodes().add(newGroup2);
        parentNode.expandEnvelope(newGroup1.getMBR());
        parentNode.expandEnvelope(newGroup2.getMBR());

        // Vérifier si le parent a trop de sous-nœuds et nécessite une division
        if (parentNode.getSubnodes().size() > this.N) {
            return split(parentNode);
        } else {
            return null;
        }

    }
    private Node[] pickSeeds(List<Node> nodes) {
        double maxWaste = Double.NEGATIVE_INFINITY;
        Node[] seeds = new Node[2];

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Envelope combinedEnvelope = new Envelope(nodes.get(i).getMBR());
                combinedEnvelope.expandToInclude(nodes.get(j).getMBR());
                double waste = combinedEnvelope.getArea() - nodes.get(i).getMBR().getArea() - nodes.get(j).getMBR().getArea();

                if (waste > maxWaste) {
                    maxWaste = waste;
                    seeds[0] = nodes.get(i);
                    seeds[1] = nodes.get(j);
                }
            }
        }

        nodes.remove(seeds[0]);
        nodes.remove(seeds[1]);

        return seeds;
    }

    private void distribute(List<Node> nodes, Node group1, Node group2) {
        while (!nodes.isEmpty()) {
            Node nodeToAssign = nodes.remove(0);

            Envelope group1Expanded = new Envelope(group1.getMBR());
            group1Expanded.expandToInclude(nodeToAssign.getMBR());
            double group1Expansion = group1Expanded.getArea() - group1.getMBR().getArea();

            Envelope group2Expanded = new Envelope(group2.getMBR());
            group2Expanded.expandToInclude(nodeToAssign.getMBR());
            double group2Expansion = group2Expanded.getArea() - group2.getMBR().getArea();

            if (group1Expansion < group2Expansion) {
                group1.getSubnodes().add(nodeToAssign);
                group1.expandEnvelope(nodeToAssign.getMBR());
            } else if (group1Expansion > group2Expansion) {
                group2.getSubnodes().add(nodeToAssign);
                group2.expandEnvelope(nodeToAssign.getMBR());
            } else {
                if (group1.getSubnodes().size() <= group2.getSubnodes().size()) {
                    group1.getSubnodes().add(nodeToAssign);
                    group1.expandEnvelope(nodeToAssign.getMBR());
                } else {
                    group2.getSubnodes().add(nodeToAssign);
                    group2.expandEnvelope(nodeToAssign.getMBR());
                }
            }
        }
    }


}

