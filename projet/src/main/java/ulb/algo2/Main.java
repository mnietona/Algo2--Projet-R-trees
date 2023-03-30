package ulb.algo2;

import java.io.File;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import java.awt.Color;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;

import java.util.Random;

import org.geotools.swing.JMapFrame;

import static java.lang.System.exit;


public class Main {
    public static void main(String[] args) throws Exception {
        //String filename="../projet/data/sh_statbel_statistical_sectors_31370_20220101.shp/sh_statbel_statistical_sectors_31370_20220101.shp";

        String filename = "../projet/data/WB_countries_Admin0_10m/WB_countries_Admin0_10m.shp";

        //String filename="../projet/data/communes-20220101-shp/communes-20220101.shp";

        File file = new File(filename);
        if (!file.exists()) throw new RuntimeException("Shapefile does not exist.");

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureCollection allFeatures = featureSource.getFeatures();
        store.dispose();

        // Build R-Trees
        final int N = 10;
        LinearRectangleTree linearTree = new LinearRectangleTree(N);
        RectangleTreeBuilder.buildTree(linearTree, allFeatures);

        QuadraticRectangleTree quadraticTree = new QuadraticRectangleTree(N);
        RectangleTreeBuilder.buildTree(quadraticTree, allFeatures);

        // Get global bounds
        ReferencedEnvelope global_bounds = featureSource.getBounds();

        GeometryBuilder gb = new GeometryBuilder();

        // Search for a random point
        Random r = new Random();
        Point p = gb.point(r.nextInt((int) global_bounds.getMinX(), (int) global_bounds.getMaxX()),
                r.nextInt((int) global_bounds.getMinY(), (int) global_bounds.getMaxY()));

        System.out.println("Point: " + p.getX() + ", " + p.getY());

        //Point p = gb.point(152183, 167679);// Plaine
        //Point p = gb.point(10.6, 59.9);// Oslo
        //Point p = gb.point(-70.9,-33.4);// Santiago
        //Point p = gb.point(169.2, -52.5);//NZ
        //Point p = gb.point(2, 45);

        Leaf linearTreeResult = linearTree.search(p);
        Leaf quadraticTreeResult = quadraticTree.search(p);

        // Linear R-Tree
        System.out.println("Linear R-Tree:");
        if (linearTreeResult != null) System.out.println(linearTreeResult.getLabel());

        // Quadratic R-Tree
        System.out.println("Quadratic R-Tree:");
        if (quadraticTreeResult != null) System.out.println(quadraticTreeResult.getLabel());


        if (linearTreeResult == null || quadraticTreeResult == null) {
            System.out.println("No result found.");
            exit(0);
        }

        // Display Map
        MapContent map = new MapContent();
        map.setTitle("Projet INFO-F203");

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        ListFeatureCollection collection = new ListFeatureCollection(featureSource.getSchema());
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureSource.getSchema());

        // Add Linear R-Tree result polygon
        collection.add(linearTreeResult.getPolygon());

        // Add Quadratic R-Tree result polygon
        collection.add(quadraticTreeResult.getPolygon());

        // Add search point
        Polygon c = gb.circle(p.getX(), p.getY(), allFeatures.getBounds().getWidth() / 200, 10);
        featureBuilder.add(c);
        collection.add(featureBuilder.buildFeature(null));

        Style style2 = SLD.createLineStyle(Color.red, 2.0f);
        Layer layer2 = new FeatureLayer(collection, style2);
        map.addLayer(layer2);
        // Now display the map
        JMapFrame.showMap(map);


    }
}