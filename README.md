# Recherche efficace de points avec R-trees

Ce projet vise à implémenter et comparer les variantes quadratique et linéaire des R-trees pour optimiser la détermination de l'appartenance de points à des polygones dans un contexte de données géospatiales et cartographiques.

## Prérequis
Pour exécuter le projet, vous devez installer les outils suivants :

- Java Development Kit (JDK) dernière version
- IntelliJ IDEA Community ou Ultimate

## Téléchargement et installation
1. Téléchargez et installez le JDK depuis le site Web d'Oracle (https://www.oracle.com/java/technologies/downloads/).
2. Téléchargez et installez la dernière version d'IntelliJ IDEA Community ou Ultimate depuis le site Web de JetBrains (https://www.jetbrains.com/idea/download/).

## Lancement du programme depuis IntelliJ
1. Ouvrez le projet dans IntelliJ. Si une fenêtre contextuelle ```"Maven build scripts found"``` apparaît, cliquez sur le bouton  
```Load Maven Project``` pour charger les dépendances Maven.
2. Pour le premier lancement :
    - Faites un clic droit sur le fichier ```src/main/java/ulb/algo2/Main``` et sélectionnez ```Run 'Main.main()'``` pour exécuter le programme.
3. Pour les lancements ultérieurs :
    - Répétez l'étape 2 ou sélectionnez "Main" dans la liste des configurations située en haut à droite de la fenêtre, puis cliquez sur le bouton ```Run 'Main'``` (flèche verte).

## Fonctionnement du programme
Une fois le programme lancé, voici les étapes qui se déroulent :

1. L'utilisateur choisit une carte parmi trois options : le monde, la Belgique et la France.
2. Le programme construit deux R-trees : un R-tree linéaire et un R-tree quadratique. Les deux arbres sont construits à l'aide des données du fichier .shp.
3. L'utilisateur est invité à choisir entre deux options :
   1. Évaluer les R-trees.
      - Le programme évalue les deux R-trees en calculant le temps d'exécution de la recherche de points. (Les points recherchés sont tirés au hasard, mais ne sont jamais null.)
   2. Afficher le résultat de la recherche d'un point sur la carte.

