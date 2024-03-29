package ulb.algo2;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Classe permettant de construire un RectangleTree.
 */
public class RectangleTreeBuilder {

    public static void buildTree(RectangleTree rectangleTree, SimpleFeatureCollection featureCollection, String map) {
        try (SimpleFeatureIterator iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                switch (map) {
                    case "Belgique" -> {
                        String label = feature.getProperty("T_SEC_FR").getValue().toString();
                        rectangleTree.insert(label, feature);
                    }
                    case "Monde" -> {
                        String label2 = feature.getProperty("NAME_FR").getValue().toString();
                        rectangleTree.insert(label2, feature);

                    }
                    case "France" -> {
                        String label3 = feature.getProperty("nom").getValue().toString();
                        rectangleTree.insert(label3, feature);
                    }
                    case "Turquie" -> {
                        String label4 = feature.getProperty("NAME_2").getValue().toString();
                        rectangleTree.insert(label4, feature);
                    }
                }

            }
        }

    }
}
