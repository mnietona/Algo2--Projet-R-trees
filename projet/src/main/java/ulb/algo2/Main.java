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

import org.opengis.feature.Property;

import java.util.Random;

import org.geotools.swing.JMapFrame;

import static java.lang.System.exit;


public class Main {
    public static void main(String[] args) throws Exception {
        String filename="../projet/data/sh_statbel_statistical_sectors_31370_20220101.shp/sh_statbel_statistical_sectors_31370_20220101.shp";

        //String filename="../projet/data/WB_countries_Admin0_10m/WB_countries_Admin0_10m.shp";

        //String filename="../projet/data/communes-20220101-shp/communes-20220101.shp";

        File file = new File(filename);
        if (!file.exists()) throw new RuntimeException("Shapefile does not exist.");

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureCollection allFeatures = featureSource.getFeatures();
        store.dispose();

        // Build R-Trees
        final int N = 5;
        LinearRectangleTree linearTree = new LinearRectangleTree(N);
        RectangleTreeBuilder.buildTree(linearTree, allFeatures);
        QuadraticRectangleTree quadraticTree = new QuadraticRectangleTree(N);
        RectangleTreeBuilder.buildTree(quadraticTree, allFeatures);


        // Get global bounds
        ReferencedEnvelope global_bounds = featureSource.getBounds();

        Random r = new Random();

        // Search for a random point
        GeometryBuilder gb = new GeometryBuilder();
        //Point p = gb.point(r.nextInt((int) global_bounds.getMinX(), (int) global_bounds.getMaxX()),
                //r.nextInt((int) global_bounds.getMinY(), (int) global_bounds.getMaxY()));


        Point p = gb.point(152183, 167679);// Plaine
        Leaf linearTreeResult = linearTree.search(p.getX(), p.getY());
        Leaf quadraticTreeResult = quadraticTree.search(p.getX(), p.getY());


        //System.out.println(linearTreeResult.getLabel()); // POur point PLaine = CAMPUS UNIVERSITAIRE

        // Test

        if (linearTreeResult != null){
            System.out.println("T'es bon gars");
            System.out.println(linearTreeResult.getPolygon().getProperties().size() + " properties found");
            exit(0);
        }else {
            System.out.println("T'es nul");
            exit(0);
        }
        // Linear R-Tree
        while (linearTreeResult == null){
            p = gb.point(r.nextInt((int) global_bounds.getMinX(), (int) global_bounds.getMaxX()),
                r.nextInt((int) global_bounds.getMinY(), (int) global_bounds.getMaxY()));
            linearTreeResult = linearTree.search(p.getX(), p.getY());
        }

        System.out.println("Linear R-Tree:");
        System.out.println(linearTreeResult.getPolygon().getProperties().size() + " properties found");
        for (Property prop : linearTreeResult.getPolygon().getProperties()) {
            if (!prop.getName().toString().equals("the_geom")) {
                System.out.println(prop.getName() + ": " + prop.getValue());
            }
        }

        // Quadratic R-Tree
        while (quadraticTreeResult == null){
            p = gb.point(r.nextInt((int) global_bounds.getMinX(), (int) global_bounds.getMaxX()),
                    r.nextInt((int) global_bounds.getMinY(), (int) global_bounds.getMaxY()));
            quadraticTreeResult = quadraticTree.search(p.getX(), p.getY());
        }

        System.out.println("Quadratic R-Tree:");
        System.out.println(quadraticTreeResult.getPolygon().getProperties().size() + " properties found");
        for (Property prop : quadraticTreeResult.getPolygon().getProperties()) {
            if (!prop.getName().toString().equals("the_geom")) {
                System.out.println(prop.getName() + ": " + prop.getValue());
            }
        }

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

/**
 * // Add your map display code here
 *         MapContent map = new MapContent();
 *         map.setTitle("Projet INFO-F203");
 *
 *         Style style = SLD.createSimpleStyle(featureSource.getSchema());
 *         Layer layer = new FeatureLayer(featureSource, style);
 *         map.addLayer(layer);
 *
 *         ListFeatureCollection collection = new ListFeatureCollection(featureSource.getSchema());
 *         SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureSource.getSchema());
 *
 *         // Add Linear R-Tree result polygon
 *         if (linearTreeResult != null) {
 *             collection.add(linearTreeResult.getPolygon());
 *         }
 *
 *         // Add Quadratic R-Tree result polygon
 *         if (quadraticTreeResult != null) {
 *             collection.add(quadraticTreeResult.getPolygon());
 *         }
 *
 *         // Add search point
 *         Polygon c = gb.circle(p.getX(), p.getY(), allFeatures.getBounds().getWidth() / 200, 10);
 *         featureBuilder.add(c);
 *         collection.add(featureBuilder.buildFeature(null));
 *
 *         Style style2 = SLD.createLineStyle(Color.red, 2.0f);
 *         Layer layer2 = new FeatureLayer(collection, style2);
 *         map.addLayer(layer2);
 *
 *         // Now display the map
 *         JMapFrame.showMap(map);
 *     }
 * }
 * */

/**
 *          // Map display
 *         MapContent map = new MapContent();
 *         map.setTitle("Projet INFO-F203");
 *
 *         Style style = SLD.createSimpleStyle(featureSource.getSchema());
 *         Layer layer = new FeatureLayer(featureSource, style);
 *         map.addLayer(layer);
 *
 *         // Create a ListFeatureCollection for Linear R-Tree result
 *         ListFeatureCollection linearCollection = new ListFeatureCollection(featureSource.getSchema());
 *         linearCollection.add(linearTreeResult.getPolygon());
 *
 *         // Create a ListFeatureCollection for Quadratic R-Tree result
 *         ListFeatureCollection quadraticCollection = new ListFeatureCollection(featureSource.getSchema());
 *         quadraticCollection.add(quadraticTreeResult.getPolygon());
 *
 *         // Add search point
 *         SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureSource.getSchema());
 *         Polygon c = gb.circle(p.getX(), p.getY(), allFeatures.getBounds().getWidth() / 200, 10);
 *         featureBuilder.add(c);
 *         linearCollection.add(featureBuilder.buildFeature(null));
 *         quadraticCollection.add(featureBuilder.buildFeature(null));
 *
 *         // Create styles for Linear and Quadratic R-Trees
 *         Style linearStyle = SLD.createLineStyle(Color.blue, 2.0f);
 *         Style quadraticStyle = SLD.createLineStyle(Color.red, 2.0f);
 *
 *         // Create polygon style for yellow polygon
 *         Style yellowPolygonStyle = SLD.createPolygonStyle(Color.black, Color.yellow, 0.0f);
 *
 *         // Create layers for Linear and Quadratic R-Trees and the yellow polygon
 *         Layer linearLayer = new FeatureLayer(linearCollection, linearStyle);
 *         Layer quadraticLayer = new FeatureLayer(quadraticCollection, quadraticStyle);
 *
 *         // Create layer for yellow polygon
 *         Layer yellowPolygonLayer = new FeatureLayer(linearCollection, yellowPolygonStyle);
 *
 *         // Add layers to the map
 *         map.addLayer(linearLayer);
 *         map.addLayer(quadraticLayer);
 *         map.addLayer(yellowPolygonLayer);
 *
 *         // Now display the map
 *         JMapFrame.showMap(map);
 */







