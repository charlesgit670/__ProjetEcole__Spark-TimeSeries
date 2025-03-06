import pandas as pd

from visualization.visualize import monitoring_today_validation


# Permet de tester le modele sur des données fixées

df_total = pd.read_csv("../data/processed/validation_groupby_JOUR.csv", sep=";", parse_dates=['JOUR'], index_col='JOUR')
df_titre = pd.read_csv("../data/processed/validation_groupby_JOUR_CATEGORIE_TITRE.csv", sep=";", parse_dates=['JOUR'], index_col='JOUR')
df_arret = pd.read_csv("../data/processed/validation_groupby_JOUR_LIBELLE_ARRET.csv", sep=";", parse_dates=['JOUR'], index_col='JOUR')

df_total = df_total[(df_total.index >= "2019-01-01") & (df_total.index <= "2019-05-30")]
df_titre = df_titre[(df_titre.index >= "2019-01-01") & (df_titre.index <= "2019-05-30")]
df_arret = df_arret[(df_arret.index >= "2019-01-01") & (df_arret.index <= "2019-05-30")]

monitoring_today_validation(df_total, df_titre, df_arret)