from pydantic import BaseModel
from typing import Optional

class UserRegister(BaseModel):
    email: str
    password: str
    role: str
    full_name: Optional[str] = None  
    tel: Optional[str] = None        

class UserLogin(BaseModel):
    email: str
    password: str

class UserResponse(BaseModel):
    id: str
    email: str
    role: str
    full_name: Optional[str] = None
    tel: Optional[str] = None
    token: str

class PasswordChange(BaseModel):
    old_password: str
    new_password: str