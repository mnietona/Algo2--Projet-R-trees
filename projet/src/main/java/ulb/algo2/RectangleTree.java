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

    protected Node split(Node node) {

        int size = node.getSubnodes().size();
        int[] seeds = pickSeeds(node.getSubnodes());

        Node group1 = new Node();
        Node group2 = new Node();
        group1.getSubnodes().add(node.getSubnodes().get(seeds[0]));
        group2.getSubnodes().add(node.getSubnodes().get(seeds[1]));
        group1.expandEnvelope(node.getSubnodes().get(seeds[0]).getMBR());
        group2.expandEnvelope(node.getSubnodes().get(seeds[1]).getMBR());

        boolean[] assigned = new boolean[size];
        assigned[seeds[0]] = true;
        assigned[seeds[1]] = true;
        int remaining = size - 2;

        while (remaining > 0) {
            int nextNodeIndex = pickNext(node.getSubnodes(), group1.getMBR(), group2.getMBR(), assigned);
            Envelope e1 = new Envelope(group1.getMBR());
            Envelope e2 = new Envelope(group2.getMBR());
            e1.expandToInclude(node.getSubnodes().get(nextNodeIndex).getMBR());
            e2.expandToInclude(node.getSubnodes().get(nextNodeIndex).getMBR());

            double area1 = group1.getMBR().getArea();
            double area2 = group2.getMBR().getArea();
            double expandedArea1 = e1.getArea();
            double expandedArea2 = e2.getArea();

            if (expandedArea1 - area1 < expandedArea2 - area2) {
                group1.getSubnodes().add(node.getSubnodes().get(nextNodeIndex));
                group1.expandEnvelope(node.getSubnodes().get(nextNodeIndex).getMBR());
            } else {
                group2.getSubnodes().add(node.getSubnodes().get(nextNodeIndex));
                group2.expandEnvelope(node.getSubnodes().get(nextNodeIndex).getMBR());
            }

            assigned[nextNodeIndex] = true;
            remaining--;
        }

        node.setSubnodes(group1.getSubnodes());
        node.setMBR(group1.getMBR());
        return group2;
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
            for (Node subnode : node.getSubnodes()) {
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

}
