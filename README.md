# Série Temporelle et Spark Streaming

## Introduction

L'objectif de ce projet de groupe est de simuler l'acquisition en streaming de données sur lesquelles seront appliqués des modèles prédictifs.  

Les données proviennent du site [Île-de-France Mobilités](https://data.iledefrance-mobilites.fr/explore/dataset/histo-validations-reseau-ferre/information/?sort=annee).  
Elles contiennent l'historique du nombre de validations de titres de transport chaque jour pour différentes stations et catégories d'abonnement (ex : Navigo, Imagine R).  

➡️ Plus de détails [ici](https://data.iledefrance-mobilites.fr/api/datasets/1.0/histo-validations-reseau-ferre/attachments/donnees_de_validation_pdf/).  

Nous transformons ces données afin de traiter trois cas différents :
- L'évolution du nombre total de validations par jour.
- L'évolution du nombre total de validations par catégorie d'abonnement et par jour.
- L'évolution du nombre total de validations par station et par jour.  

Ces transformations sont effectuées avec Spark Streaming et écrites dans trois dossiers distincts au fur et à mesure, afin de simuler une acquisition de données en temps réel.  

Ensuite, un modèle prédictif utilisant l'historique des 45 dernières mesures estime le nombre de validations pour le jour suivant.  
L'objectif est de comparer les valeurs prédites avec les valeurs réelles afin de détecter d'éventuelles anomalies.  

---

## Structure du Projet

📁 **`data/processed`** : Décompressez le fichier `data.rar` pour récupérer des données prêtes à l'utilisation.  
📁 **`modelisation/`** : Contient toute la partie modèle et monitoring.  
📁 **`IDFM_Spark/`** : Contient la partie Spark.  

---

## Spark Streaming

📜 **`script.scala`**  
Ce script récupère le fichier de données dans `data/firstCSV/`, le décompose et le reconstruit progressivement dans `data/csv/`.  
Toutes les 20 secondes, il ajoute 7 nouvelles lignes jusqu'à reconstituer le fichier de données initial.  

📜 **`SparkStream.scala`**  
Ce script lit en continu les fichiers dans `data/csv/` avec une fenêtre de 1 minute d'intervalle, effectue différentes opérations de regroupement et écrit les résultats dans `data/output/`.  
Il utilise un **watermark** de 1 minute pour mieux gérer les retards potentiels et éviter les données erronées.  
📌 [Explication du watermarking](https://www.databricks.com/blog/feature-deep-dive-watermarking-apache-spark-structured-streaming)  

---

## Modèle Prédictif

Nous utilisons des **réseaux de neurones récurrents (RNN)**, particulièrement adaptés à l'analyse des séries temporelles.  
Les modèles sont entraînés sur **4 ans d'historique (2015-2019)** et testés sur l'année suivante.  
Nous avons délibérément exclu les données correspondant à la pandémie de COVID-19.  

🖼️ **Visualisation des résultats**  
![model_test](imgs%2Fmodel_test.png)  

- Axe vertical : Nombre total de validations sur tout le réseau.  
- Axe horizontal : Jours de l'année 2019.  
- Les valeurs réelles et les prédictions sont superposées.  

📌 Une prédiction pour le jour *x* nécessite les **45 dernières valeurs**.  
Ainsi, le premier jour affiché correspond au **jour 46**, et les données sont tronquées au **1er décembre**, juste avant la pandémie.  

➡️ **Première observation** : Les prédictions semblent bien coïncider avec les valeurs réelles, les variations saisonnières étant correctement capturées.  

🔍 **Zoom sur une période de 2 semaines (jours 58 à 72)**  
![zoom_model_test](imgs%2Fzoom_model_test.png)  

- Pendant la **première semaine**, les prédictions suivent bien les variations réelles.  
- On observe une hausse prévue par le modèle à partir du **lundi suivant**, qui ne s'est pas réalisée (probablement un jour férié).  
- **Ajout d'un indicateur "jour férié"** dans le modèle : pas d'amélioration notable.  
📌 **Le modèle reste perfectible.**  

---

## Monitoring  

L'objectif final est de présenter un **dashboard récapitulatif** pour mettre en évidence les anomalies.  

🔢 **Calcul de la différence entre valeurs réelles et prédites :**  
- On soustrait la **marge d'erreur moyenne** du modèle.  
- Cette marge est calculée en moyennant l'erreur entre valeurs réelles et prédites sur le jeu de test, pour chaque jour, type de titre et station.  
✅ Cela améliore la **confiance dans la détection des anomalies**.  

### 📅 **Exemple du 29 mai 2019 (mercredi - jour ouvré)**
![monitoring_normalDay](imgs%2Fmonitoring_normalDay.png)  

📊 **Graphiques :**  
- **Haut** : Top 5 des plus grandes différences par station.  
- **Bas gauche** : Différence entre le nombre total de validations sur le réseau.  
- **Bas droit** : Nombre total de validations par type d'abonnement.  

➡️ **Analyse** :  
Tout semble normal sauf **trois stations** affichant des validations plus faibles que prévu (travaux ou incidents ?).  

### 📅 **Exemple du 30 mai 2019 (jeudi - jour férié)**
![monitoring_closedDay](imgs%2Fmonitoring_closedDay.png)  

➡️ **Analyse** :  
Les écarts sont **très importants** ! Étant donné qu'il s'agit d'un jour férié, les valeurs réelles sont bien **plus basses** que celles prédites.
