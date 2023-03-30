package ulb.algo2;

import org.locationtech.jts.geom.Envelope;

import java.util.List;


public class LinearRectangleTree extends RectangleTree {
    public LinearRectangleTree(int N) {
        super(N);
    }

    @Override
    protected Node split(Node node) {

        // 1. Trouver les deux MBR les plus éloignés selon un axe choisi (par exemple, l'axe des x)
        int[] seeds = pickSeeds(node.getSubnodes());

        // 2. Créer deux nouveaux nœuds et attribuer les MBR sélectionnés à chacun d'eux
        Node newNode1 = new Node();
        Node newNode2 = new Node();
        newNode1.getSubnodes().add(node.getSubnodes().get(seeds[0]));
        newNode2.getSubnodes().add(node.getSubnodes().get(seeds[1]));

        // 3. Mettre à jour les MBR des nouveaux nœuds
        newNode1.expandEnvelope(node.getSubnodes().get(seeds[0]).getMBR());
        newNode2.expandEnvelope(node.getSubnodes().get(seeds[1]).getMBR());

        // 4. Supprimer les MBR sélectionnés de la liste des sous-nœuds
        node.getSubnodes().remove(seeds[1]);
        node.getSubnodes().remove(seeds[0]);

        // 5. Assigner les sous-nœuds restants aux deux nouveaux nœuds
        while (!node.getSubnodes().isEmpty()) {
            Node remainingNode = node.getSubnodes().get(0);
            Envelope remainingMBR = remainingNode.getMBR();

            Envelope mbr1Expanded = new Envelope(newNode1.getMBR());
            mbr1Expanded.expandToInclude(remainingMBR);
            double cost1 = mbr1Expanded.getArea() - newNode1.getMBR().getArea();

            Envelope mbr2Expanded = new Envelope(newNode2.getMBR());
            mbr2Expanded.expandToInclude(remainingMBR);
            double cost2 = mbr2Expanded.getArea() - newNode2.getMBR().getArea();

            if (cost1 < cost2) {
                newNode1.getSubnodes().add(remainingNode);
                newNode1.expandEnvelope(remainingMBR);
            } else {
                newNode2.getSubnodes().add(remainingNode);
                newNode2.expandEnvelope(remainingMBR);
            }

            node.getSubnodes().remove(0);
        }

        // 6. Mettre à jour les sous-nœuds du nœud d'origine
        node.getSubnodes().add(newNode1);
        node.getSubnodes().add(newNode2);

        // 7. Mettre à jour le MBR du nœud d'origine
        node.getMBR().init();
        node.expandEnvelope(newNode1.getMBR());
        node.expandEnvelope(newNode2.getMBR());

        return newNode2;
    }

    private int[] pickSeeds(List<Node> subnodes) {
        int size = subnodes.size();
        double maxWaste = Double.NEGATIVE_INFINITY;
        int[] seeds = new int[2];

        for (int i = 0; i < size; i++) {
            Envelope e1 = subnodes.get(i).getMBR();
            for (int j = i + 1; j < size; j++) {
                Envelope e2 = subnodes.get(j).getMBR();
                Envelope combinedEnvelope = new Envelope(e1);
                combinedEnvelope.expandToInclude(e2);

                double waste = combinedEnvelope.getArea() - e1.getArea() - e2.getArea();
                if (waste > maxWaste) {
                    maxWaste = waste;
                    seeds[0] = i;
                    seeds[1] = j;
                }
            }
        }
        return seeds;
    }
}


