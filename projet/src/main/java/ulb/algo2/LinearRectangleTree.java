package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;


public class LinearRectangleTree extends RectangleTree {
    public LinearRectangleTree(int N) {
        super(N);
    }


    @Override
    public int[] pickSeeds(List<Node> subnodes){
        int leftIndex = 0;
        int rightIndex = subnodes.size() - 1;
        int[] seeds = new int[2];

        // Trouver l'indice de l'enfant le plus à gauche (leftIndex)
        for (int i = 1; i < subnodes.size(); i++) {
            if (subnodes.get(i).getMBR().getMinX() < subnodes.get(leftIndex).getMBR().getMinX()) {
                leftIndex = i;
            }
        }

        // Trouver l'indice de l'enfant le plus à droite (rightIndex)
        for (int i = subnodes.size() - 2; i >= 0; i--) {
            if (subnodes.get(i).getMBR().getMaxX() > subnodes.get(rightIndex).getMBR().getMaxX()) {
                rightIndex = i;
            }
        }

        // Retourner les indices des enfants les plus à gauche et à droite
        return new int[]{leftIndex, rightIndex};
    }


}


