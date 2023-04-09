package ulb.algo2;

import org.locationtech.jts.geom.Envelope;
import java.util.List;

public class LinearRectangleTree extends RectangleTree {
    public LinearRectangleTree(int N) {
        super(N);
    }

    @Override
    public int[] pickSeeds(List<Node> subnodes){
        int leftMostIndex = 0;
        int rightMostIndex = 0;

        for (int i = 1; i < subnodes.size(); i++) {
            // Trouver l'indice du rectangle ayant le côté droit avec le plus petit x (leftMostIndex)
            if (subnodes.get(i).getMBR().getMaxX() < subnodes.get(leftMostIndex).getMBR().getMaxX()) {
                leftMostIndex = i;
            }
            // Trouver l'indice du rectangle ayant le côté gauche avec le plus grand x (rightMostIndex)
            if (subnodes.get(i).getMBR().getMinX() > subnodes.get(rightMostIndex).getMBR().getMinX()) {
                rightMostIndex = i;
            }
        }

        // Retourner les indices des rectangles les plus à gauche et à droite
        return new int[]{leftMostIndex, rightMostIndex};
    }
}
