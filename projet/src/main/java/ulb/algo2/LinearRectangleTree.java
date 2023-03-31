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
        int[] seeds = new int[2];

        double min_x = Double.POSITIVE_INFINITY;
        double max_x = Double.NEGATIVE_INFINITY;
        double min_y = Double.POSITIVE_INFINITY;
        double max_y = Double.NEGATIVE_INFINITY;
        int seed1_x = -1, seed2_x = -1, seed1_y = -1, seed2_y = -1;

        // Find the MBR and maximum extents for each dimension
        for (int i = 0; i < size; i++) {
            Envelope mbr = subnodes.get(i).getMBR();

            if (mbr.getMinX() < min_x) {
                min_x = mbr.getMinX();
                seed1_x = i;
            }
            if (mbr.getMaxX() > max_x) {
                max_x = mbr.getMaxX();
                seed2_x = i;
            }
            if (mbr.getMinY() < min_y) {
                min_y = mbr.getMinY();
                seed1_y = i;
            }
            if (mbr.getMaxY() > max_y) {
                max_y = mbr.getMaxY();
                seed2_y = i;
            }
        }

        // Calculate the extents for each dimension
        double extentX = Math.abs(max_x - min_x);
        double extentY = Math.abs(max_y - min_y);

        // Choose the dimension with the largest extent and return the seeds
        if (extentX > extentY) {
            seeds[0] = seed1_x;
            seeds[1] = seed2_x;
        } else {
            seeds[0] = seed1_y;
            seeds[1] = seed2_y;
        }
        return seeds;
    }


}


