package ulb.algo2;

import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;

/**
 * Classe représentant une feuille de l'arbre R.
 *
 */
public class Leaf extends Node {
    private String label;
    private SimpleFeature polygon;

    /**
     * Constructeur de la classe Leaf.
     *
     * @param label est le label de la feuille
     * @param polygon est le polygone de la feuille
     */
    public Leaf(String label, SimpleFeature polygon) {
        super();
        this.label = label;
        this.polygon = polygon;
        // Obtenir le MBR du polygone
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        this.mbr = geometry.getEnvelopeInternal();
    }

    public String getLabel() {
        return this.label;
    }

    public SimpleFeature getPolygon() {
        return this.polygon;
    }

}
