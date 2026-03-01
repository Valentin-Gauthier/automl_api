from pydantic import BaseModel

class CarFeatures(BaseModel):
    brand: str
    model: str
    age: int
    kms: float
    fuel_type: str
    transmission: str
    ext_col: str
    int_col: str
    accident: str
    clean_title: str
    engine_hp: float
    engine_liters: float
    engine_cyl: float 