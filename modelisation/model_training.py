from models.model import find_and_save_complete_name, model_total_validation, model_titre_validation, model_arret_validation


# find_and_save_complete_name("../data/processed/validation_groupby_JOUR_CATEGORIE_TITRE.csv", "CATEGORIE_TITRE")
# find_and_save_complete_name("../data/processed/validation_groupby_JOUR_LIBELLE_ARRET.csv", "LIBELLE_ARRET")
model_total_validation("../data/processed/validation_groupby_JOUR.csv", is_model_load=True, plot=True)
# model_titre_validation("../data/processed/validation_groupby_JOUR_CATEGORIE_TITRE.csv", is_model_load=False, plot=True)
# model_arret_validation("../data/processed/validation_groupby_JOUR_LIBELLE_ARRET.csv", is_model_load=False, plot=False)
