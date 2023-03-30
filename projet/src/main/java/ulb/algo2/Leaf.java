package ulb.algo2;

import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;


public class Leaf extends Node {
    private String label;
    private SimpleFeature polygon;

    public Leaf(String label, SimpleFeature polygon) {
        super();
        this.label = label;
        this.polygon = polygon;
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
