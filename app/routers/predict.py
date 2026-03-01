from fastapi import APIRouter, HTTPException
import joblib
import pandas as pd
import os
from app.schemas import CarFeatures

router = APIRouter(
    prefix="/predict",
    tags=["Prediction"]
)

MODEL_PATH = "/code/models/first_model_cars.pkl"

# Chargement du model
automl_model = None
if os.path.exists(MODEL_PATH):
    try:
        automl_model = joblib.load(MODEL_PATH)
        print(f"Modèle charger avec succès")
    except Exception as e:
        print(f"Erreur lors du chargement du modèle : {e}")



@router.post("/")
def predict(car : CarFeatures):
    if automl_model is None:
        raise HTTPException(status_code=500, detail="Modèle indisponible.")
    
    df = pd.DataFrame([car.model_dump()])
    df.columns = [f"feature_{i}" for i in range(df.shape[1])]

    try:
        prediction = automl_model.predict(df)
        return {
            "status" : "sucess",
            "estimated_price": float(prediction[0])
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Erreur de prédiction :{str(e)}")