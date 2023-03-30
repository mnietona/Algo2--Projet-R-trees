package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;


public class QuadraticRectangleTree extends RectangleTree {
    public QuadraticRectangleTree(int N) {
        super(N);
    }

    @Override
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

    private int[] pickSeeds(List<Node> subnodes) {
        int size = subnodes.size();
        double maxWaste = Double.NEGATIVE_INFINITY;
        int[] seeds = new int[2];

        for (int i = 0; i < size; i++) {
            Envelope e1 = subnodes.get(i).getMBR();
            for (int j = i + 1; j < size; j++) {
                Envelope e2 = subnodes.get(j).getMBR();
                Envelope combinedEnvelope = new Envelope(e1);
                combinedEnvelope.expandToInclude(e2);

                double waste = combinedEnvelope.getArea() - e1.getArea() - e2.getArea();
                if (waste > maxWaste) {
                    maxWaste = waste;
                    seeds[0] = i;
                    seeds[1] = j;
                }
            }
        }
        return seeds;
    }

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




}

