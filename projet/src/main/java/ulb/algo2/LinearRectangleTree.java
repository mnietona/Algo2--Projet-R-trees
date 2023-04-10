package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.awt.*;
import java.util.List;

public class LinearRectangleTree extends RectangleTree {
    public LinearRectangleTree(int N) {
        super(N);
    }

    @Override
    public int[] pickSeeds(List<Node> subnodes) {
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
