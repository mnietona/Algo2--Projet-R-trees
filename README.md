# Projet R-trees

Ce projet vise à implémenter et comparer les variantes quadratique et linéaire des R-trees pour la recherche d'un point. 

## Prérequis
Pour exécuter le projet, vous devez installer les outils suivants :

- Java Development Kit (JDK) dernière version
- IntelliJ IDEA Community ou Ultimate

## Téléchargement et installation
1. Téléchargez et installez le JDK depuis le site Web d'Oracle (https://www.oracle.com/java/technologies/downloads/).
2. Téléchargez et installez la dernière version d'IntelliJ IDEA Community ou Ultimate depuis le site Web de JetBrains (https://www.jetbrains.com/idea/download/).

## Ajout des données 
Pour utiliser les données cartographiques, ajoutez dans dossier ```data``` qui se trouve dans le dossier ```projet``` les trois cartes fournies dans l'énoncé du projet.
La 4ème carte ```Turquie``` est déjà ajoutée.

## Lancement du programme depuis IntelliJ
1. Ouvrez le projet dans IntelliJ. Si une fenêtre contextuelle ```"Maven build scripts found"``` apparaît, cliquez sur le bouton  
```Load Maven Project``` pour charger les dépendances Maven.
2. Pour le premier lancement :
    - Faites un clic droit sur le fichier ```src/main/java/ulb/algo2/Main``` et sélectionnez ```Run 'Main.main()'``` pour exécuter le programme.
3. Pour les lancements ultérieurs :
    - Répétez l'étape 2 ou sélectionnez "Main" dans la liste des configurations située en haut à droite de la fenêtre, puis cliquez sur le bouton ```Run 'Main'``` (flèche verte).

## Fonctionnement du programme
Une fois le programme lancé, voici les étapes qui se déroulent :

1. L'utilisateur choisit une carte parmi quatres options : le monde, la Belgique, la France et la Turquie .
2. Le programme construit deux R-trees à l'aide des données du fichier .shp:
      -  ```R-tree linéaire```
      -  ```R-tree quadratique.```
   
3. L'utilisateur est invité à choisir entre deux options : 
    - Évaluer les R-trees sur des points aléatoires : 
      - Le programme évalue les deux R-trees en calculant le temps d'exécution de la méthode ```search```.
      (Les points recherchés sont tirés au hasard et ne sont jamais null)
    - Affichez les résultats de la recherche d'un point pour les deux arbres (linéaire et quadratique) sur des cartes distinctes.
