from fastapi import APIRouter, Depends, BackgroundTasks
from app.routers.auth import get_current_admin_user
import os
import joblib
import json
from app.schemas import TrainParams

from src.nevergrad.automl import AutoML

router = APIRouter(
    prefix="/admin",
    tags=["Administration"]
)

def train_model(params: TrainParams):
    print(f"Lancement de l'entrainement du modele avec un budget de {params.budget}", flush=True)
    try:
        DATA_PATH = "/code/data/cars/cars.data"

        if not os.path.exists(DATA_PATH):
            print(f"Dataset introuvable : {DATA_PATH}", flush=True)
            return
        param_dict = params.model_dump()
        automl = AutoML(**param_dict)
        automl.fit(DATA_PATH)
        automl.eval()

        joblib.dump(automl, "/code/models/first_model_cars.pkl")

        # sauvegarde des performances
        metrics = {
            "date_entrainement": __import__("datetime").datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "meilleur_modele": str(type(automl.best_model).__name__),
            "parametres_gagnants": str(automl.best_params),
            "rappel_task_type": str(automl.task_type)
        }
        with open("/code/models/latest_metrics.json", "w", encoding="utf-8") as f:
            json.dump(metrics, f, indent=4)


        print(f"Entrainement terminé ! Le model à été sauvegarder : /code/models/first_model_cars.pkl", flush=True)

    except Exception as e:
        print(f"Erreur lors de l'entrainement : {e}", flush=True)


@router.get("/performance")
def get_model_performance(admin_username:str = Depends(get_current_admin_user)):
    metrics_path = "/code/models/latest_metrics.json"

    if not os.path.exists(metrics_path):
        return {
            "status": "warning",
            "message": "Aucun entraînement n'a encore été terminé, ou le fichier de métriques est introuvable."
        }

    try:
        with open(metrics_path, "r", encoding="utf-8") as f:
            metrics_data = json.load(f)
        
        return {
            "status": "success",
            "message": f"Voici les performances du dernier entraînement.",
            "data": metrics_data
        }
    
    except Exception as e:
        return {
            "status": "error",
            "message": f"Erreur lors de la lecture des métriques : {e}"
        }

@router.post("/train")
def retrain_model(params : TrainParams, background_tasks: BackgroundTasks, admin_username: str = Depends(get_current_admin_user)):

    background_tasks.add_task(train_model, params)
    return {
        "status": "success", 
        "message": f"Serrure déverrouillée avec succès par l'admin '{admin_username}' ! L'entraînement de l'AutoML peut commencer."
    }