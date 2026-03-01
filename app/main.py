from fastapi import FastAPI
from app.routers import predict

app = FastAPI(
    title="API_AutoML",
    description="API permettant l'utilisation de l'AutoML",
    version="1.0.0"
)

app.include_router(predict.router)

@app.get("/")
def root():
    return {"message": "L'API est en ligne !"}