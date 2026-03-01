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

class UserCreate(BaseModel):
    username: str
    password: str
    is_admin: bool = False

class Token(BaseModel):
    access_token: str
    token_type: str

class TrainParams(BaseModel):
    test_size: float = 0.2
    random_state: int = 42
    scoring: str = "auto"
    n_folds: int = 3
    feature_selection_threshold: float = 99999.0 
    budget: int = 2
    num_workers: int = 2
    timeout_min: int = 1
    mem_gb: int = 2
    cpus_per_task: int = 1
    cluster_partition: str = "gpu"
    verbose: bool = True