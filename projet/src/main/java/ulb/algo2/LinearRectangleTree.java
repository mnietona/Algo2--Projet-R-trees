package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

/**
 * Classe représentant un LinearRectangleTree.
 * Cette classe hérite de la classe RectangleTree.
 * Elle implémente les méthodes pickSeeds et calculateCost.
 */
public class LinearRectangleTree extends RectangleTree {
    public LinearRectangleTree(int N) {
        super(N);
    }

    @Override
    protected int[] pickSeeds(List<Node> subnodes) {
        double maxDiffX = Double.NEGATIVE_INFINITY;
        double maxDiffY = Double.NEGATIVE_INFINITY;
        int indexX1 = 0;
        int indexX2 = 0;
        int indexY1 = 0;
        int indexY2 = 0;

        for (int i = 0; i < subnodes.size(); i++) {
            Envelope mbr = subnodes.get(i).getMBR();
            double diffX = mbr.getWidth();
            double diffY = mbr.getHeight();

            // Si on a un nouveau max pour l'axe X, on met à jour les index
            if (diffX > maxDiffX) {
                maxDiffX = diffX;
                indexX1 = i;
                indexX2 = (i + 1) % subnodes.size();
            }
            // Si on a un nouveau max pour l'axe Y, on met à jour les index
            if (diffY > maxDiffY) {
                maxDiffY = diffY;
                indexY1 = i;
                indexY2 = (i + 1) % subnodes.size();
            }
        }
        return maxDiffX > maxDiffY ? new int[]{indexX1, indexX2} : new int[]{indexY1, indexY2};
    }

    @Override
    protected double calculateCost(Envelope mbr1, Envelope mbr2, Envelope currentMBR) {
        double cost1 = calculateExpansionCost(mbr1, currentMBR);
        double cost2 = calculateExpansionCost(mbr2, currentMBR);
        // On retourne la différence entre les deux coûts
        return Math.abs(cost1 - cost2);
    }
}
