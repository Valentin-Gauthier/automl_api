from sqlalchemy.orm import Session
from passlib.context import CryptContext
import app.db_models as db_models
import app.schemas as schemas

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def get_user_by_username(db: Session, username: str):
    return db.query(db_models.User).filter(db_models.User.username  == username).first()

def create_user(db: Session, user: schemas.UserCreate):
    hashed_password = pwd_context.hash(user.password)
    db_user = db_models.User(
        username=user.username, 
        hashed_password=hashed_password, 
        is_admin=user.is_admin
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)