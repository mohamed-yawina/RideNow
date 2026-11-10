from datetime import datetime, timedelta, timezone
from jose import JWTError, jwt
from passlib.context import CryptContext
import os
from dotenv import load_dotenv

load_dotenv()

SECRET_KEY = os.getenv("SECRET_KEY", "").strip()
if len(SECRET_KEY) < 32 or SECRET_KEY in {
    "votre_secret_key_tres_long_et_securise_ici",
    "replace_with_a_long_random_jwt_signing_secret",
}:
    raise RuntimeError(
        "SECRET_KEY must be set to a unique, unpredictable value of at least 32 characters."
    )
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24  # 24 heures

# Configuration bcrypt avec limite explicite
pwd_context = CryptContext(
    schemes=["bcrypt"],
    deprecated="auto",
    bcrypt__rounds=12  # Nombre de rounds (défaut=12)
)

def verify_password(plain_password, hashed_password):
    # Tronquer le mot de passe à 72 caractères (limite bcrypt)
    plain_password = plain_password[:72]
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password):
    # Tronquer le mot de passe à 72 caractères
    password = password[:72]
    return pwd_context.hash(password)

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def decode_token(token: str):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except JWTError:
        return None