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
        System.out.println("3: Turquie");
        int choice = scanner.nextInt();

        String filename = "";
        String map = "";
        int N = 10;

        switch (choice) {
            case 0 -> {
                filename = "projet/data/WB_countries_Admin0_10m/WB_countries_Admin0_10m.shp";
                map = "World";
                System.out.println("the map : " + map);
            }
            case 1 -> {
                filename = "projet/data/sh_statbel_statistical_sectors_31370_20220101.shp/sh_statbel_statistical_sectors_31370_20220101.shp";
                map = "Belgium";
                System.out.println("the map : " + map);
            }
            case 2 -> {
                filename = "projet/data/communes-20220101-shp/communes-20220101.shp";
                map = "France";
                System.out.println("the map : " + map);
            }
            case 3 -> {
                filename = "projet/data/TUR_adm/TUR_adm2.shp";
                map = "Turquie";
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
        System.out.println("Building R-Trees...");
        long startTime = System.nanoTime();
        LinearRectangleTree linearTree = new LinearRectangleTree(N);
        RectangleTreeBuilder.buildTree(linearTree, allFeatures, map);
        long endTime = System.nanoTime();
        System.out.println("Linear R-Tree built in " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms.");

        QuadraticRectangleTree quadraticTree = new QuadraticRectangleTree(N);
        RectangleTreeBuilder.buildTree(quadraticTree, allFeatures, map);
        endTime = System.nanoTime();
        System.out.println("Quadratic R-Trees built in " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms.\n");

        // Get global bounds
        ReferencedEnvelope global_bounds = featureSource.getBounds();
        GeometryBuilder gb = new GeometryBuilder();

        System.out.println("Choose an option:");
        System.out.println("0: Evaluate the R-Trees");
        System.out.println("1: Display the R-Trees");
        choice = scanner.nextInt();
        if (choice == 0) {
            evaluateRtreeVariants(allFeatures,linearTree,quadraticTree,global_bounds, gb,map);
        }else if (choice == 1) {
            mapResult(map, featureSource, allFeatures, linearTree, quadraticTree, global_bounds, gb);
        }else{
            System.out.println("Wrong choice");
            exit(0);
        }

    }

    private static void mapResult(String map, SimpleFeatureSource featureSource, SimpleFeatureCollection allFeatures, LinearRectangleTree linearTree, QuadraticRectangleTree quadraticTree, ReferencedEnvelope global_bounds, GeometryBuilder gb) {
        Pair<Point, String> randomPoint = getRandomPoint(gb, global_bounds, allFeatures, map);
        Leaf linearTreeResult = linearTree.search(randomPoint.getLeft());
        Leaf quadraticTreeResult = quadraticTree.search(randomPoint.getLeft());
        System.out.println("Random point: " + randomPoint.getLeft().toString());
        System.out.println("Label of the random point: " + randomPoint.getRight());

        if (linearTreeResult != null) {
            System.out.println("Linear R-Tree result: " + linearTreeResult.getLabel());
            showMapForTree(featureSource, linearTreeResult, gb, allFeatures, randomPoint.getLeft(), Color.red, 2.0f, "Linear R-Tree");
        }else {
            System.out.println("Linear R-Tree result: null");
        }

        if (quadraticTreeResult != null) {
            System.out.println("Quadratic R-Tree result: " + quadraticTreeResult.getLabel());
            showMapForTree(featureSource, quadraticTreeResult, gb, allFeatures, randomPoint.getLeft(), Color.blue, 4.0f, "Quadratic R-Tree");
        }else {
            System.out.println("Quadratic R-Tree result: null");
        }


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
                        case "Turquie" -> label = target.getProperty("NAME_2").getValue().toString();
                    }
                }

            }
        }
        return  Pair.of(p, label);
    }


    public static void evaluateRtreeVariants(SimpleFeatureCollection allFeatures,LinearRectangleTree linearTree,
                                             QuadraticRectangleTree quadraticTree, ReferencedEnvelope global_bounds, GeometryBuilder gb,String map) {
        int nQueries = 100;
        List <Pair<Point,String>> linearOK = new ArrayList<>();
        List <Pair<Point,String>> quadraticOK = new ArrayList<>();

        System.out.println("Generating "+ nQueries +" random points...");

        // Génère des points aléatoires
        List<Pair<Point,String>> testPoints = new ArrayList<>();
        for (int i = 0; i < nQueries; i++) {
            Pair<Point ,String> result  = getRandomPoint(gb, global_bounds, allFeatures, map);
            testPoints.add(result);
        }
        System.out.println("Random points generated.\n");

        // Evaluation pour Linear R-Tree
        calculTime(linearTree, nQueries, linearOK, testPoints);

        // Evaluation pour Quadratic R-Tree
        calculTime(quadraticTree, nQueries, quadraticOK, testPoints);

    }

    private static void calculTime(RectangleTree Tree, int nQueries, List<Pair<Point, String>> treeLabel, List<Pair<Point, String>> testPoints) {
        long startTime;
        long elapsedTime;
        startTime = System.nanoTime();
        if (Tree instanceof LinearRectangleTree)
            System.out.println("Search Linear R-Tree:");
        else
            System.out.println("Search Quadratic R-Tree:");

        for (Pair<Point, String> pair : testPoints) {
            Leaf result = Tree.search(pair.getLeft());
            if (result != null) {
                if (result.getLabel().equals(pair.getRight())) {
                    treeLabel.add(pair);
                }
                else {
                    System.out.println("Wrong result for point " + pair.getLeft());
                }
            }

        }
        elapsedTime = System.nanoTime() - startTime;
        System.out.println("Time elapsed: " + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms");
        System.out.println("Results found: " + treeLabel.size() + " sur " + nQueries+"\n");
    }


    public static void showMapForTree(SimpleFeatureSource featureSource, Leaf treeResult,
                                      GeometryBuilder gb, SimpleFeatureCollection allFeatures, Point p,
                                      Color treeColor, float treeStrokeWidth, String treeTitle) {
        // Display Map
        MapContent map = new MapContent();
        map.setTitle(treeTitle);

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        ListFeatureCollection collection = new ListFeatureCollection(featureSource.getSchema());
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureSource.getSchema());

        SimpleFeature tree = treeResult.getPolygon();

        // Add tree result MBR
        featureBuilder.add(gb.box(tree.getBounds().getMinX(),
                tree.getBounds().getMinY(),
                tree.getBounds().getMaxX(),
                tree.getBounds().getMaxY()
        ));
        collection.add(featureBuilder.buildFeature(null));

        // Add search point (uncomment and customize if needed)
        Polygon c = gb.circle(p.getX(), p.getY(), allFeatures.getBounds().getWidth() / 200, 10);
        featureBuilder.add(c);
        collection.add(featureBuilder.buildFeature(null));

        // couleur pour l'arbre
        Style treeStyle = SLD.createLineStyle(treeColor, treeStrokeWidth);

        Layer treeLayer = new FeatureLayer(collection, treeStyle);
        map.addLayer(treeLayer);

        // Now display the map
        JMapFrame.showMap(map);

    }

}