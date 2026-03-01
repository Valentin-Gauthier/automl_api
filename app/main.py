from fastapi import FastAPI

app = FastAPI(
    title="API_AutoML",
    description="API permettant l'utilisation de l'AutoML",
    version="1.0.0"
)

@app.get("/")
def root():
    return {"message": "L'API est en ligne !"}