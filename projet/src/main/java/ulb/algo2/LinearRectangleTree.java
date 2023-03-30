package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

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
}


