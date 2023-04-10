package ulb.algo2;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

public class RectangleTreeBuilder {

    public static void buildTree(RectangleTree rectangleTree, SimpleFeatureCollection featureCollection, String map) {
        try (SimpleFeatureIterator iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                switch (map) {
                    case "Belgium" -> {
                        String label = feature.getProperty("T_SEC_FR").getValue().toString();
                        rectangleTree.insert(label, feature);
                    }
                    case "World" -> {
                        String label2 = feature.getProperty("NAME_FR").getValue().toString();
                        rectangleTree.insert(label2, feature);

                    }
                    case "France" -> {
                        String label3 = feature.getProperty("nom").getValue().toString();
                        rectangleTree.insert(label3, feature);
                    }
                }

            }
        }

    }
}
