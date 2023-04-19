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
        int[] maxIndices = findIndicesWithMaxDiffs(subnodes);

        seeds[0] = maxIndices[0] > maxIndices[1] ? maxIndices[2] : maxIndices[3];
        seeds[1] = findIndexWithMinOverlap(subnodes, seeds[0]);

        return seeds;
    }

    private int[] findIndicesWithMaxDiffs(List<Node> subnodes) {
        double maxDiffX = Double.NEGATIVE_INFINITY;
        double maxDiffY = Double.NEGATIVE_INFINITY;
        int maxIndexX = 0;
        int maxIndexY = 0;

        for (int i = 0; i < subnodes.size(); i++) {
            Envelope mbr = subnodes.get(i).getMBR();
            double diffX = mbr.getWidth();
            double diffY = mbr.getHeight();

            if (diffX > maxDiffX) {
                maxDiffX = diffX;
                maxIndexX = i;
            }
            if (diffY > maxDiffY) {
                maxDiffY = diffY;
                maxIndexY = i;
            }
        }

        return new int[]{(int) maxDiffX, (int) maxDiffY, maxIndexX, maxIndexY};
    }

    private int findIndexWithMinOverlap(List<Node> subnodes, int maxIndex) {
        double minOverlap = Double.MAX_VALUE;
        int minIndex = 0;

        for (int i = 0; i < subnodes.size(); i++) {
            if (i == maxIndex) continue;

            Envelope e1 = subnodes.get(maxIndex).getMBR();
            Envelope e2 = subnodes.get(i).getMBR();
            double overlap = calculateWaste(e1, e2);

            if (overlap < minOverlap) {
                minOverlap = overlap;
                minIndex = i;
            }
        }

        return minIndex;
    }

    @Override
    protected double calculateCost(Envelope mbr1, Envelope mbr2, Envelope currentMBR) {
        return Math.abs(calculateExpansionCost(mbr1, currentMBR) - calculateExpansionCost(mbr2, currentMBR));
    }
}
