package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

/**
 * Classe représentant un QuadraticRectangleTree.
 * Cette classe hérite de la classe RectangleTree.
 * Elle implémente les méthodes pickSeeds et calculateCost.
 */
public class QuadraticRectangleTree extends RectangleTree {
    public QuadraticRectangleTree(int N) {
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
                double waste = calculateWaste(e1, subnodes.get(j).getMBR());
                // Si le gaspillage est plus grand que le gaspillage maximum, on le remplace
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
    protected double calculateCost(Envelope mbr1, Envelope mbr2, Envelope currentMBR) {
        double cost1 = calculateExpansionCost(mbr1, currentMBR);
        double cost2 = calculateExpansionCost(mbr2, currentMBR);
        // On retourne le carré de la somme des coûts
        return cost1 * cost1 + cost2 * cost2;
    }
}

