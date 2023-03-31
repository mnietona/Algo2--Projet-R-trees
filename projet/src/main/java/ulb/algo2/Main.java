package ulb.algo2;

import java.io.File;

import org.apache.commons.lang3.tuple.Pair;
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
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;

import java.util.ArrayList;
import java.util.Random;

import org.geotools.swing.JMapFrame;

import static java.lang.System.exit;


import java.util.List;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class Main {
    public static void main(String[] args) throws Exception {

        // input choice
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose a map:");
        System.out.println("0: World");
        System.out.println("1: Belgium");
        System.out.println("2: France");
        int choice = scanner.nextInt();

        String filename = "";
        String map = "";
        switch (choice) {
            case 0 -> {
                filename = "../projet/data/WB_countries_Admin0_10m/WB_countries_Admin0_10m.shp";
                map = "World";
                System.out.println("the map : " + map);
            }
            case 1 -> {
                filename = "../projet/data/sh_statbel_statistical_sectors_31370_20220101.shp/sh_statbel_statistical_sectors_31370_20220101.shp";
                map = "Belgium";
                System.out.println("the map : " + map);
            }
            case 2 -> {
                filename = "../projet/data/communes-20220101-shp/communes-20220101.shp";
                map = "France";
                System.out.println("the map : " + map);
            }
            default -> {
                System.out.println("Wrong choice");
                exit(0);
            }
        }

        File file = new File(filename);
        if (!file.exists()) throw new RuntimeException("Shapefile does not exist.");

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureCollection allFeatures = featureSource.getFeatures();
        store.dispose();


        // Build R-Trees

        final int N = 100;
        LinearRectangleTree linearTree = new LinearRectangleTree(N);
        RectangleTreeBuilder.buildTree(linearTree, allFeatures, map);
        QuadraticRectangleTree quadraticTree = new QuadraticRectangleTree(N);
        RectangleTreeBuilder.buildTree(quadraticTree, allFeatures, map);


        // Get global bounds
        ReferencedEnvelope global_bounds = featureSource.getBounds();
        GeometryBuilder gb = new GeometryBuilder();
        evaluateRtreeVariants(allFeatures,linearTree,quadraticTree,global_bounds, gb,map);


        Leaf linearTreeResult = null;
        Leaf quadraticTreeResult = null;
        Point p = null;

        while(linearTreeResult == null || quadraticTreeResult == null) {
            Pair<Point ,String> pair = getRandomPoint(gb, global_bounds, allFeatures,map);
            if (linearTreeResult == null) {
                linearTreeResult = linearTree.search(pair.getLeft());
            }
            if (quadraticTreeResult == null) {
                quadraticTreeResult = quadraticTree.search(pair.getLeft());
            }
        }

        // Linear R-Tree
        System.out.println("Linear R-Tree:");
        System.out.println(linearTreeResult.getLabel());

        // Quadratic R-Tree
        System.out.println("Quadratic R-Tree:");
        System.out.println(quadraticTreeResult.getLabel());


        if (linearTreeResult.getLabel().equals(quadraticTreeResult.getLabel())) {
            System.out.println("The two trees returned the same result.");
        }else{
            System.out.println("The two trees returned different results.");
        }


        // Show results on map
        showMap(featureSource,linearTreeResult,quadraticTreeResult, gb, allFeatures, p);

    }

    public static void evaluateRtreeVariants(SimpleFeatureCollection allFeatures,LinearRectangleTree linearTree,
                                             QuadraticRectangleTree quadraticTree, ReferencedEnvelope global_bounds, GeometryBuilder gb,String map) {
        int nQueries = 100;
        long startTime, elapsedTime;
        List <Pair<Point,String>> linearOK = new ArrayList<>();
        List <Pair<Point,String>> quadraticOK = new ArrayList<>();


        // Générer une liste de points à tester
        List<Pair<Point,String>> testPoints = new ArrayList<>();
        for (int i = 0; i < nQueries; i++) {
            Pair<Point ,String> result  = getRandomPoint(gb, global_bounds, allFeatures, map);
            testPoints.add(result);
        }

        // Evaluation pour Linear R-Tree
        startTime = System.nanoTime();
        System.out.println("Linear R-Tree:");
        for (Pair<Point, String> pair : testPoints) {
            Leaf result = linearTree.search(pair.getLeft());
            if (result != null) {
                linearOK.add(pair);
            }

        }
        elapsedTime = System.nanoTime() - startTime;
        System.out.println("Time elapsed: " + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms");
        System.out.println("Results found: " + linearOK.size());

        // Evaluation pour Quadratic R-Tree
        startTime = System.nanoTime();
        System.out.println("Quadratic R-Tree:");
        for (Pair<Point, String> pair : testPoints) {
            Leaf result = quadraticTree.search(pair.getLeft());
            if (result != null) {
                quadraticOK.add(pair);
            }

        }
        elapsedTime = System.nanoTime() - startTime;
        System.out.println("Time elapsed: " + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms");
        System.out.println("Results found: " + quadraticOK.size());

        exit(0);
    }


    public static void showMap( SimpleFeatureSource featureSource, Leaf linearTreeResult, Leaf quadraticTreeResult,
                                GeometryBuilder gb, SimpleFeatureCollection allFeatures, Point p) {
        // Display Map
        MapContent map = new MapContent();
        map.setTitle("Projet INFO-F203");

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        ListFeatureCollection collection = new ListFeatureCollection(featureSource.getSchema());
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureSource.getSchema());
        SimpleFeatureBuilder featureBuilder2 = new SimpleFeatureBuilder(featureSource.getSchema());

        SimpleFeature linear = linearTreeResult.getPolygon();
        SimpleFeature quadratic = quadraticTreeResult.getPolygon();

        //Add linear R-Tree result MBR
        featureBuilder.add(gb.box(linear.getBounds().getMinX(),
                linear.getBounds().getMinY(),
                linear.getBounds().getMaxX(),
                linear.getBounds().getMaxY()
        ));
        collection.add(featureBuilder.buildFeature(null));

        // Add quadratic R-Tree result MBR
        featureBuilder2.add(gb.box(quadratic.getBounds().getMinX(),
                quadratic.getBounds().getMinY(),
                quadratic.getBounds().getMaxX(),
                quadratic.getBounds().getMaxY()
        ));
        collection.add(featureBuilder2.buildFeature(null));

        // Add search point (uncomment and customize if needed)
        Polygon c = gb.circle(p.getX(), p.getY(), allFeatures.getBounds().getWidth() / 200, 10);
        featureBuilder.add(c);
        collection.add(featureBuilder.buildFeature(null));

        // couleur pour quadrtic tree
        Style style2 = SLD.createLineStyle(Color.blue, 4.0f);
        // couleur pour linear tree
        Style style3 = SLD.createLineStyle(Color.red, 2.0f);

        Layer layer2 = new FeatureLayer(collection, style2);
        Layer layer3 = new FeatureLayer(collection, style3);
        map.addLayer(layer2);
        map.addLayer(layer3);

        // Now display the map
        JMapFrame.showMap(map);
    }

    public static Pair<Point,String> getRandomPoint(GeometryBuilder gb, ReferencedEnvelope global_bounds, SimpleFeatureCollection allFeatures,String map) {
        Random r = new Random();
        Point p = null;
        SimpleFeature target=null;
        String label = "";

        while (target == null) {

            p = gb.point(r.nextInt((int) global_bounds.getMinX(), (int) global_bounds.getMaxX()),
                    r.nextInt((int) global_bounds.getMinY(), (int) global_bounds.getMaxY()));
            try ( SimpleFeatureIterator iterator = allFeatures.features() ){
                while( iterator.hasNext()){
                    SimpleFeature feature = iterator.next();

                    MultiPolygon polygon = (MultiPolygon) feature.getDefaultGeometry();

                    if (polygon != null && polygon.contains(p)) {
                        target = feature;
                        break;
                    }
                }
                if (target != null) {
                    switch (map) {
                        case "Belgium" -> label = target.getProperty("T_SEC_FR").getValue().toString();
                        case "World" -> label = target.getProperty("NAME_FR").getValue().toString();
                        case "France" -> label = target.getProperty("nom").getValue().toString();
                    }
                    System.out.println(label);
                }

            }
        }
        return  Pair.of(p, label);
    }



}