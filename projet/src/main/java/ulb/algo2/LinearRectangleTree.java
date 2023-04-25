package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

public class LinearRectangleTree extends RectangleTree {
    public LinearRectangleTree(int N) {
        super(N);
    }

    @Override
    protected int[] pickSeeds(List<Node> subnodes) {
        double maxDiffX = Double.NEGATIVE_INFINITY;
        double maxDiffY = Double.NEGATIVE_INFINITY;
        int maxIndexX1 = 0;
        int maxIndexX2 = 0;
        int maxIndexY1 = 0;
        int maxIndexY2 = 0;

        for (int i = 0; i < subnodes.size(); i++) {
            Envelope mbr = subnodes.get(i).getMBR();
            double diffX = mbr.getWidth();
            double diffY = mbr.getHeight();

            if (diffX > maxDiffX) {
                maxDiffX = diffX;
                maxIndexX1 = i;
                maxIndexX2 = (i + 1) % subnodes.size();
            }
            if (diffY > maxDiffY) {
                maxDiffY = diffY;
                maxIndexY1 = i;
                maxIndexY2 = (i + 1) % subnodes.size();
            }
        }

        return maxDiffX > maxDiffY ? new int[]{maxIndexX1, maxIndexX2} : new int[]{maxIndexY1, maxIndexY2};
    }

    @Override
    protected double calculateCost(Envelope mbr1, Envelope mbr2, Envelope currentMBR) {
        return Math.abs(calculateExpansionCost(mbr1, currentMBR) - calculateExpansionCost(mbr2, currentMBR));
    }
}
