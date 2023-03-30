package ulb.algo2;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeature;

import java.util.List;

public abstract class RectangleTree {
    protected int N;
    protected Node root;

    public RectangleTree(int N) {
        this.N = N;
    }

    public abstract void insert(String label, SimpleFeature polygon);

    public abstract Leaf search(double x, double y);

    protected abstract Node chooseNode(Node node, Envelope polygonEnvelope);

    protected abstract Node addLeaf(Node node, String label, SimpleFeature polygon);

    protected abstract Node split(Node node);
}
