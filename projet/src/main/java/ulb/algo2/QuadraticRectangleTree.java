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
    @Override
    public int pickNext(List<Node> subNodes, Envelope mbr1, Envelope mbr2, boolean[] assigned) {
        int nextNodeIndex = -1;
        double maxCost = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < subNodes.size(); i++) {
            // Ignorez les nœuds déjà attribués
            if (assigned[i]) {
                continue;
            }

            // Récupérez le MBR du nœud actuel
            Envelope currentMBR = subNodes.get(i).getMBR();

            // Calculez le coût d'expansion pour inclure le nœud actuel dans les MBR de chaque groupe
            Envelope mbr1Expanded = new Envelope(mbr1);
            mbr1Expanded.expandToInclude(currentMBR);
            double cost1 = mbr1Expanded.getArea() - mbr1.getArea();

            Envelope mbr2Expanded = new Envelope(mbr2);
            mbr2Expanded.expandToInclude(currentMBR);
            double cost2 = mbr2Expanded.getArea() - mbr2.getArea();

            // Calculez le coût quadratique pour inclure le nœud actuel
            double cost = cost1 * cost1 + cost2 * cost2;

            // Mettez à jour le nœud suivant à attribuer si le coût quadratique est plus grand que la valeur précédente
            if (cost > maxCost) {
                maxCost = cost;
                nextNodeIndex = i;
            }
        }

        return nextNodeIndex;
    }








}

