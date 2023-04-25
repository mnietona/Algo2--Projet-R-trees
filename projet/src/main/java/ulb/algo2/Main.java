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
        System.out.println("Quelle carte voulez-vous ?");
        System.out.println("0: Monde");
        System.out.println("1: Belgique");
        System.out.println("2: France");
        System.out.println("3: Turquie");
        int choice = scanner.nextInt();

        String filename = "";
        String map = "";
        int N = 10;

        switch (choice) {
            case 0 -> {
                filename = "projet/data/WB_countries_Admin0_10m/WB_countries_Admin0_10m.shp";
                map = "Monde";

            }
            case 1 -> {
                filename = "projet/data/sh_statbel_statistical_sectors_31370_20220101.shp/sh_statbel_statistical_sectors_31370_20220101.shp";
                map = "Belgique";
            }
            case 2 -> {
                filename = "projet/data/communes-20220101-shp/communes-20220101.shp";
                map = "France";
            }
            case 3 -> {
                filename = "projet/data/TUR_adm/TUR_adm2.shp";
                map = "Turquie";
            }
        }
        System.out.println("Votre choix : " + map);

        File file = new File(filename);
        if (!file.exists()) throw new RuntimeException("Le fichier n'existe pas.");

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureCollection allFeatures = featureSource.getFeatures();
        store.dispose();


        // Build R-Trees
        System.out.println("Construction des R-Trees...");
        long startTime = System.nanoTime();
        LinearRectangleTree linearTree = new LinearRectangleTree(N);
        RectangleTreeBuilder.buildTree(linearTree, allFeatures, map);
        long endTime = System.nanoTime();
        System.out.println("R-Tree linéaire construit en " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms.");

        QuadraticRectangleTree quadraticTree = new QuadraticRectangleTree(N);
        RectangleTreeBuilder.buildTree(quadraticTree, allFeatures, map);
        endTime = System.nanoTime();
        System.out.println("R-Tree quadratique construit en  " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms.\n");

        // Get global bounds
        ReferencedEnvelope global_bounds = featureSource.getBounds();
        GeometryBuilder gb = new GeometryBuilder();

        System.out.println("Choisissez une action:");
        System.out.println("0: Evaluer les R-Trees sur des points aléatoires");
        System.out.println("1: Afficher le résultat d'un point aléatoire");
        choice = scanner.nextInt();
        if (choice == 0) {
            evaluateRtreeVariants(allFeatures,linearTree,quadraticTree,global_bounds, gb,map);
        }else if (choice == 1) {
            mapResult(map, featureSource, allFeatures, linearTree, quadraticTree, global_bounds, gb);
        }
        exit(0);

    }

    private static void mapResult(String map, SimpleFeatureSource featureSource, SimpleFeatureCollection allFeatures, LinearRectangleTree linearTree, QuadraticRectangleTree quadraticTree, ReferencedEnvelope global_bounds, GeometryBuilder gb) {
        Pair<Point, String> randomPoint = getRandomPoint(gb, global_bounds, allFeatures, map);
        System.out.println("Point : " + randomPoint.getLeft().toString() + " Nom du point : " + randomPoint.getRight());
        Leaf linearTreeResult = linearTree.search(randomPoint.getLeft());
        Leaf quadraticTreeResult = quadraticTree.search(randomPoint.getLeft());

        if (linearTreeResult != null) {
            System.out.println("Résultat du R-Tree linéaire : " + linearTreeResult.getLabel());
            showMapForTree(featureSource, linearTreeResult, gb, allFeatures, randomPoint.getLeft(), Color.red, 2.0f, "Linear R-Tree");
        }else {
            System.out.println("Résultat du R-Tree linéaire est null");
        }

        if (quadraticTreeResult != null) {
            System.out.println("Résultat du R-Tree quadratique : " + quadraticTreeResult.getLabel());
            showMapForTree(featureSource, quadraticTreeResult, gb, allFeatures, randomPoint.getLeft(), Color.blue, 4.0f, "Quadratic R-Tree");
        }else {
            System.out.println("Résultat du R-Tree quadratique est null");
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
                        case "Belgique" -> label = target.getProperty("T_SEC_FR").getValue().toString();
                        case "Monde" -> label = target.getProperty("NAME_FR").getValue().toString();
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

        System.out.println("Génération de " + nQueries + " points aléatoires...");

        // Génère des points aléatoires
        List<Pair<Point,String>> testPoints = new ArrayList<>();
        for (int i = 0; i < nQueries; i++) {
            Pair<Point ,String> result  = getRandomPoint(gb, global_bounds, allFeatures, map);
            testPoints.add(result);
        }
        System.out.println("Points aléatoires générés.\n");

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
            System.out.println("Recherche R-Tree linéaire:");
        else
            System.out.println("Recherche R-Tree quadratique:");

        for (Pair<Point, String> pair : testPoints) {
            Leaf result = Tree.search(pair.getLeft());
            if (result != null) {
                if (result.getLabel().equals(pair.getRight())) {
                    treeLabel.add(pair);
                }
                else {
                    System.out.println("Erreur de résultat pour le point : " + pair.getLeft().toString());
                }
            }

        }
        elapsedTime = System.nanoTime() - startTime;
        System.out.println("Temps d'exécution : " + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + " ms");
        System.out.println("Résultats corrects : " + treeLabel.size() + "/" + nQueries + "\n");
    }


    public static void showMapForTree(SimpleFeatureSource featureSource, Leaf treeResult,
                                      GeometryBuilder gb, SimpleFeatureCollection allFeatures, Point p,
                                      Color treeColor, float treeStrokeWidth, String treeTitle) {
        MapContent map = new MapContent();
        map.setTitle(treeTitle);

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        ListFeatureCollection collection = new ListFeatureCollection(featureSource.getSchema());
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureSource.getSchema());

        SimpleFeature tree = treeResult.getPolygon();

        featureBuilder.add(gb.box(tree.getBounds().getMinX(),
                tree.getBounds().getMinY(),
                tree.getBounds().getMaxX(),
                tree.getBounds().getMaxY()
        ));
        collection.add(featureBuilder.buildFeature(null));

        Polygon c = gb.circle(p.getX(), p.getY(), allFeatures.getBounds().getWidth() / 200, 10);
        featureBuilder.add(c);
        collection.add(featureBuilder.buildFeature(null));

        Style treeStyle = SLD.createLineStyle(treeColor, treeStrokeWidth);

        Layer treeLayer = new FeatureLayer(collection, treeStyle);
        map.addLayer(treeLayer);

        JMapFrame.showMap(map);

    }

}