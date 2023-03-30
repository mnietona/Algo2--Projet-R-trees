package ulb.algo2;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

public class RectangleTreeBuilder {

    public static void buildTree(RectangleTree rectangleTree, SimpleFeatureCollection featureCollection) {
        SimpleFeatureIterator iterator = featureCollection.features();
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();
            String label = feature.getID();
            rectangleTree.insert(label, feature);
        }
        iterator.close();
    }
}
