from fastapi import FastAPI
from app.database import engine
import app.db_models as db_models
from app.routers import predict, auth

app = FastAPI(
    title="API_AutoML",
    description="API permettant l'utilisation de l'AutoML",
    version="1.0.0"
)

db_models.Base.metadata.create_all(bind=engine)

app.include_router(predict.router)
app.include_router(auth.router)

@app.get("/")
def root():
    return {"message": "L'API est en ligne !"}