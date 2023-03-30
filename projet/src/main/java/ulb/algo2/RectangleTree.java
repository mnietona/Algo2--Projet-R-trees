package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Point;


public abstract class RectangleTree {
    protected int N;
    protected Node root;

    public RectangleTree(int N) {
        this.N = N;
    }

    private Node getRoot() {
        return root;
    }

    public void insert(String label, SimpleFeature polygon) {
        if (root == null) {
            root = new Leaf(label, polygon);
        } else {
            Node newNode = addLeaf(root, label, polygon);
            if (newNode != null) {
                // La racine a été divisée, créer un nouveau nœud racine
                Node newRoot = new Node();
                newRoot.getSubnodes().add(root);
                newRoot.getSubnodes().add(newNode);
                root = newRoot;
            }
        }
    }


    protected Node chooseNode(Node node, SimpleFeature polygon) {
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        Envelope polygonEnvelope = geometry.getEnvelopeInternal();

        if (node.getSubnodes().isEmpty() || node.getSubnodes().get(0) instanceof Leaf) {
            return node;
        }

        double minExpansion = Double.MAX_VALUE;
        Node bestNode = null;

        for (Node subnode : node.getSubnodes()) {
            Envelope expandedEnvelope = new Envelope(subnode.getMBR());
            expandedEnvelope.expandToInclude(polygonEnvelope);
            double expansion = expandedEnvelope.getArea() - subnode.getMBR().getArea();

            if (expansion < minExpansion) {
                minExpansion = expansion;
                bestNode = subnode;
            } else if (expansion == minExpansion) {
                assert bestNode != null;
                if (subnode.getMBR().getArea() < bestNode.getMBR().getArea()) {
                    bestNode = subnode;
                }
            }
        }

        assert bestNode != null;
        return chooseNode(bestNode, polygon);
    }




    protected Node addLeaf(Node node, String label, SimpleFeature polygon) {
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        Envelope polygonEnvelope = geometry.getEnvelopeInternal();

        if (node.getSubnodes().isEmpty() || node.getSubnodes().get(0) instanceof Leaf) {
            node.getSubnodes().add(new Leaf(label, polygon));
        } else {
            Node selectedNode = chooseNode(node, polygon);
            Node newNode = addLeaf(selectedNode, label, polygon);
            if (newNode != null) {
                node.getSubnodes().add(newNode);
            }
        }
        node.expandEnvelope(polygonEnvelope);

        if (node.getSubnodes().size() >= N) {
            return split(node);
        } else {
            return null;
        }
    }



    public Leaf search(Point point) {
        return searchHelper(this.getRoot(), point);
    }

    private Leaf searchHelper(Node node, Point point) {
        if (node == null) {
            return null;
        }
        if (node instanceof Leaf leaf) {
            if (leaf.getMBR().contains(point.getX(), point.getY()) &&
                    ((Geometry) leaf.getPolygon().getDefaultGeometry()).contains(point)) {
                return leaf;
            }
        } else {
            for (Node subnode : node.getSubnodes()) {
                if (subnode.getMBR().contains(point.getX(), point.getY())) {
                    Leaf result = searchHelper(subnode, point);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    protected abstract Node split(Node node);



}
