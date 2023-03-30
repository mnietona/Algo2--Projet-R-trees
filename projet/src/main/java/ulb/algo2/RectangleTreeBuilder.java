package ulb.algo2;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

public class RectangleTreeBuilder {

    public static void buildTree(RectangleTree rectangleTree, SimpleFeatureCollection featureCollection) {
        try (SimpleFeatureIterator iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                String label =  feature.getProperty("T_SEC_FR").getValue().toString();
                //String label = feature.getProperty("NAME_FR").getValue().toString();
                rectangleTree.insert(label, feature);
            }
        }
    }
}
