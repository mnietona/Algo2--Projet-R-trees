package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

public class LinearRectangleTree extends RectangleTree {
    public LinearRectangleTree(int N) {
        super(N);
    }

    @Override
    protected int[] pickSeeds(List<Node> subnodes) {
        int[] seeds = new int[2];
        double maxDiffX = Double.NEGATIVE_INFINITY;
        double maxDiffY = Double.NEGATIVE_INFINITY;
        int maxIndexX = 0;
        int maxIndexY = 0;

        for (int i = 0; i < subnodes.size(); i++) {
            double minX = subnodes.get(i).getMBR().getMinX();
            double maxX = subnodes.get(i).getMBR().getMaxX();
            double minY = subnodes.get(i).getMBR().getMinY();
            double maxY = subnodes.get(i).getMBR().getMaxY();

            double diffX = maxX - minX;
            double diffY = maxY - minY;

            if (diffX > maxDiffX) {
                maxDiffX = diffX;
                maxIndexX = i;
            }

            if (diffY > maxDiffY) {
                maxDiffY = diffY;
                maxIndexY = i;
            }
        }

        int maxIndex;

        // Choose the axis with the greatest total difference
        if (maxDiffX > maxDiffY) {
            maxIndex = maxIndexX;
        } else {
            maxIndex = maxIndexY;
        }

        seeds[0] = maxIndex;

        double minOverlap = Double.MAX_VALUE;
        int minIndex = 0;

        for (int i = 0; i < subnodes.size(); i++) {
            if (i != maxIndex) {
                Envelope tempMBR = new Envelope(subnodes.get(maxIndex).getMBR());
                tempMBR.expandToInclude(subnodes.get(i).getMBR());
                double overlap = tempMBR.getArea() - subnodes.get(maxIndex).getMBR().getArea() - subnodes.get(i).getMBR().getArea();

                if (overlap < minOverlap) {
                    minOverlap = overlap;
                    minIndex = i;
                }
            }
        }

        seeds[1] = minIndex;

        return seeds;
    }



    @Override
    public int pickNext(List<Node> subNodes, Envelope mbr1, Envelope mbr2, boolean[] assigned) {
        int nextNodeIndex = -1;
        // Met maxcostDiff a une valeur negative et tres tres petie pour qu'un noeud soit prit
        double maxCostDiff = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < subNodes.size(); i++) {
            if (assigned[i]) {
                continue;
            }
            Envelope currentMBR = subNodes.get(i).getMBR();
            // Calcul le nouvel air après ajout du MBR

            Envelope mbr1Expanded = new Envelope(mbr1);
            mbr1Expanded.expandToInclude(currentMBR);
            double cost1 = mbr1Expanded.getArea() - mbr1.getArea();

            Envelope mbr2Expanded = new Envelope(mbr2);
            mbr2Expanded.expandToInclude(currentMBR);
            double cost2 = mbr2Expanded.getArea() - mbr2.getArea();

            // Calcul la diff des air des deux MBR
            double costDiff = Math.abs(cost1 - cost2);

            if (costDiff > maxCostDiff) {
                maxCostDiff = costDiff;
                nextNodeIndex = i;
            }
        }

        return nextNodeIndex;
    }

}
