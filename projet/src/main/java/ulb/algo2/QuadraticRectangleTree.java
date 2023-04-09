package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;


public class QuadraticRectangleTree extends RectangleTree {
    public QuadraticRectangleTree(int N) {
        super(N);
    }

    @Override
    // Cette fonction sélectionne deux sous-nœuds d'une liste en utilisant l'algorithme de sélection de graines du R-Tree.
    public int[] pickSeeds(List<Node> subnodes) {
        // Obtenir la taille de la liste des sous-nœuds
        int size = subnodes.size();
        // Initialiser la variable maxWaste pour garder une trace du gaspillage d'espace maximal
        double maxWaste = Double.NEGATIVE_INFINITY;
        // Initialiser un tableau d'entiers pour stocker les indices des deux graines sélectionnées
        int[] seeds = new int[2];

        // Parcourir tous les sous-nœuds dans la liste
        for (int i = 0; i < size; i++) {
            // Récupérer le rectangle englobant (MBR) du sous-nœud actuel
            Envelope e1 = subnodes.get(i).getMBR();
            // Parcourir les sous-nœuds restants dans la liste
            for (int j = i + 1; j < size; j++) {
                // Récupérer le MBR du sous-nœud suivant
                Envelope e2 = subnodes.get(j).getMBR();
                // Créer un nouveau MBR qui englobe les deux MBRs e1 et e2
                Envelope combinedEnvelope = new Envelope(e1);
                combinedEnvelope.expandToInclude(e2);

                // Calculer le gaspillage d'espace, c'est-à-dire la différence entre l'aire du MBR combiné et les aires des deux MBRs individuels
                double waste = combinedEnvelope.getArea() - e1.getArea() - e2.getArea();
                // Si le gaspillage d'espace est supérieur au gaspillage maximal enregistré, mettre à jour maxWaste et stocker les indices des sous-nœuds
                if (waste > maxWaste) {
                    maxWaste = waste;
                    seeds[0] = i;
                    seeds[1] = j;
                }
            }
        }
        // Retourner les indices des deux graines sélectionnées
        return seeds;
    }







}

