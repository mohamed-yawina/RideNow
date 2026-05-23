from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from supabase_client import supabase
from auth import verify_password, get_password_hash, create_access_token, decode_token
from models import UserRegister, UserLogin, UserResponse, PasswordChange
from math import radians, sin, cos, sqrt, atan2
from typing import Optional
from datetime import datetime, timedelta

app = FastAPI(title="RideNow API")

# CORS pour Android
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ========== FONCTION DE CALCUL DE DISTANCE ==========
def calculate_distance(lat1, lon1, lat2, lon2):
    """Calcule la distance entre deux points GPS en km"""
    R = 6371
    lat1_rad = radians(lat1)
    lat2_rad = radians(lat2)
    delta_lat = radians(lat2 - lat1)
    delta_lon = radians(lon2 - lon1)
    
    a = sin(delta_lat / 2) ** 2 + cos(lat1_rad) * cos(lat2_rad) * sin(delta_lon / 2) ** 2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return R * c

# ========== DÉPENDANCE POUR VÉRIFIER LE TOKEN ==========
async def get_current_user(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Token manquant")
    
    token = authorization.replace("Bearer ", "")
    payload = decode_token(token)
    
    if not payload:
        raise HTTPException(status_code=401, detail="Token invalide")
    
    return payload

# ========== ROUTE DE TEST ==========
@app.get("/")
def root():
    return {"message": "RideNow API sécurisée"}

# ========== AUTHENTIFICATION ==========

@app.post("/register")
def register(user: UserRegister):
    existing = supabase.table("users").select("*").eq("email", user.email).execute()
    
    if existing.data:
        raise HTTPException(status_code=400, detail="Email déjà utilisé")
    
    if user.role not in ["client", "driver"]:
        raise HTTPException(status_code=400, detail="Rôle invalide")
    
    hashed_password = get_password_hash(user.password)
    
    user_data = {
        "email": user.email,
        "password": hashed_password,
        "role": user.role
    }
    
    if hasattr(user, 'full_name') and user.full_name:
        user_data["full_name"] = user.full_name
    
    if hasattr(user, 'tel') and user.tel:
        user_data["tel"] = user.tel
    
    new_user = supabase.table("users").insert(user_data).execute()
    user_id = new_user.data[0]["id"]
    
    if user.role == "driver":
        supabase.table("drivers").insert({
            "user_id": user_id,
            "latitude": 0,
            "longitude": 0,
            "available": False,
            "rating": 0,
            "vehicle_type": "voiture"
        }).execute()
    
    token = create_access_token({
        "sub": user_id,
        "email": user.email,
        "role": user.role
    })
    
    return {
        "message": "Utilisateur créé",
        "user_id": user_id,
        "email": user.email,
        "role": user.role,
        "full_name": user_data.get("full_name", ""),
        "tel": user_data.get("tel", ""),
        "token": token
    }

@app.post("/login")
def login(user: UserLogin):
    result = supabase.table("users").select("*").eq("email", user.email).execute()
    
    if not result.data:
        raise HTTPException(status_code=401, detail="Email ou mot de passe incorrect")
    
    db_user = result.data[0]
    
    if not verify_password(user.password, db_user["password"]):
        raise HTTPException(status_code=401, detail="Email ou mot de passe incorrect")
    
    token = create_access_token({
        "sub": db_user["id"],
        "email": db_user["email"],
        "role": db_user["role"]
    })
    
    return {
        "message": "Connexion réussie",
        "user_id": db_user["id"],
        "email": db_user["email"],
        "role": db_user["role"],
        "full_name": db_user.get("full_name", ""),
        "token": token
    }

@app.get("/profile")
def get_profile(current_user = Depends(get_current_user)):
    user_id = current_user.get("sub")
    
    result = supabase.table("users").select("id, email, role").eq("id", user_id).execute()
    
    if not result.data:
        raise HTTPException(status_code=404, detail="Utilisateur non trouvé")
    
    return result.data[0]

@app.post("/change-password")
def change_password(
    password_data: PasswordChange,
    current_user = Depends(get_current_user)
):
    user_id = current_user.get("sub")
    
    result = supabase.table("users").select("*").eq("id", user_id).execute()
    db_user = result.data[0]
    
    if not verify_password(password_data.old_password, db_user["password"]):
        raise HTTPException(status_code=401, detail="Ancien mot de passe incorrect")
    
    new_hashed = get_password_hash(password_data.new_password)
    
    supabase.table("users").update({"password": new_hashed}).eq("id", user_id).execute()
    
    return {"message": "Mot de passe changé avec succès"}

@app.post("/logout")
def logout(current_user = Depends(get_current_user)):
    return {"message": "Déconnexion réussie"}

# ========== GESTION DES CHAUFFEURS ==========

@app.post("/driver/register")
def register_driver(
    vehicle_type: str = "voiture",
    current_user = Depends(get_current_user)
):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Seuls les chauffeurs peuvent s'enregistrer comme tels")
    
    user_id = current_user.get("sub")
    
    if vehicle_type not in ["taxi", "voiture", "moto"]:
        raise HTTPException(status_code=400, detail="Type de véhicule invalide")
    
    existing = supabase.table("drivers").select("*").eq("user_id", user_id).execute()
    
    if existing.data:
        supabase.table("drivers")\
            .update({"vehicle_type": vehicle_type})\
            .eq("user_id", user_id)\
            .execute()
    else:
        supabase.table("drivers").insert({
            "user_id": user_id,
            "latitude": 0,
            "longitude": 0,
            "available": False,
            "rating": 0,
            "vehicle_type": vehicle_type
        }).execute()
    
    return {"message": "Chauffeur enregistré avec succès", "vehicle_type": vehicle_type}

@app.put("/driver/update-vehicle")
def update_driver_vehicle(
    vehicle_type: str,
    current_user = Depends(get_current_user)
):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    if vehicle_type not in ["taxi", "voiture", "moto"]:
        raise HTTPException(status_code=400, detail="Type de véhicule invalide")
    
    user_id = current_user.get("sub")
    
    supabase.table("drivers")\
        .update({"vehicle_type": vehicle_type})\
        .eq("user_id", user_id)\
        .execute()
    
    return {"message": "Type de véhicule mis à jour", "vehicle_type": vehicle_type}

# ========== GÉOLOCALISATION ==========

@app.get("/drivers/nearby")
def get_nearby_drivers(
    lat: float, 
    lng: float, 
    vehicle_type: Optional[str] = None, 
    current_user = Depends(get_current_user)
):
    query = supabase.table("drivers")\
        .select("*, users!inner(email, id, full_name)")\
        .eq("available", True)
    
    if vehicle_type and vehicle_type != "tous" and vehicle_type != "voiture":
        query = query.eq("vehicle_type", vehicle_type)
    
    result = query.execute()
    drivers = result.data
    
    nearby_drivers = []
    
    for driver in drivers:
        driver_lat = driver.get("latitude", 0)
        driver_lng = driver.get("longitude", 0)
        
        if driver_lat == 0 and driver_lng == 0:
            continue
        
        distance = calculate_distance(lat, lng, driver_lat, driver_lng)
        
        if distance <= 10:
            user_info = driver.get("users", {})
            if user_info is None:
                user_info = {}
            
            nearby_drivers.append({
                "driver_id": driver["user_id"],
                "latitude": driver_lat,
                "longitude": driver_lng,
                "distance_km": round(distance, 2),
                "rating": driver.get("rating", 0),
                "email": user_info.get("email", "Chauffeur"),
                "full_name": user_info.get("full_name", "Chauffeur"),
                "vehicle_type": driver.get("vehicle_type", "voiture")
            })
    
    nearby_drivers.sort(key=lambda x: x["distance_km"])
    
    return {"drivers": nearby_drivers[:10]}

@app.post("/driver/location")
def update_driver_location(
    lat: float, 
    lng: float, 
    available: bool = None,
    current_user = Depends(get_current_user)
):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Seuls les chauffeurs peuvent mettre à jour leur position")
    
    user_id = current_user.get("sub")
    
    update_data = {
        "latitude": lat,
        "longitude": lng
    }
    
    if available is not None:
        update_data["available"] = available
    
    supabase.table("drivers")\
        .update(update_data)\
        .eq("user_id", user_id)\
        .execute()
    
    return {"message": "Position mise à jour", "lat": lat, "lng": lng}

@app.get("/driver/status")
def get_driver_status(current_user = Depends(get_current_user)):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    user_id = current_user.get("sub")
    
    result = supabase.table("drivers")\
        .select("available, latitude, longitude, rating, vehicle_type")\
        .eq("user_id", user_id)\
        .execute()
    
    if not result.data:
        supabase.table("drivers").insert({
            "user_id": user_id,
            "latitude": 0,
            "longitude": 0,
            "available": False,
            "rating": 4.5,
            "vehicle_type": "voiture"
        }).execute()
        return {
            "available": False,
            "latitude": 0,
            "longitude": 0,
            "rating": 4.5,
            "vehicle_type": "voiture"
        }
    
    return result.data[0]

@app.post("/driver/status")
def update_driver_status(
    request_data: dict,
    current_user = Depends(get_current_user)
):
    """Met à jour le statut du chauffeur (en ligne/hors ligne)"""
    
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    user_id = current_user.get("sub")
    available = request_data.get("available", False)
    
    supabase.table("drivers")\
        .update({"available": available})\
        .eq("user_id", user_id)\
        .execute()
    
    return {"message": "Statut mis à jour", "available": available}

@app.get("/driver/available-rides")
def get_available_rides(current_user = Depends(get_current_user)):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    
    driver_info = supabase.table("drivers").select("vehicle_type").eq("user_id", driver_id).execute()
    if not driver_info.data:
        raise HTTPException(status_code=404, detail="Chauffeur non trouvé")
    driver_vehicle = driver_info.data[0].get("vehicle_type", "voiture")
    
    declined = supabase.table("ride_declines").select("ride_id").eq("driver_id", driver_id).execute()
    declined_ids = [d["ride_id"] for d in declined.data] if declined.data else []
    
    result = supabase.table("rides")\
        .select("*, client:client_id(id, email, full_name)")\
        .eq("status", "en attente")\
        .eq("vehicle_type", driver_vehicle)\
        .is_("driver_id", "null")\
        .execute()
    
    available = [r for r in result.data if r["id"] not in declined_ids]
    
    print(f"📊 Courses disponibles pour {driver_id}: {len(available)}")
    
    formatted = []
    for ride in available:
        client = ride.get("client", {})
        formatted.append({
            "id": ride["id"],
            "client_name": client.get("full_name", client.get("email", "Client").split("@")[0]),
            "destination": ride["destination"],
            "vehicle_type": ride.get("vehicle_type", "voiture"),
            "offer": ride.get("offer", 30),
            "origin_address": ride.get("origin_address", "Position actuelle")
        })
    
    return {"rides": formatted}

# ========== MATCHING ET COURSES ==========

@app.post("/ride/match/{ride_id}")
def match_ride(ride_id: str, current_user = Depends(get_current_user)):
    """Chauffeur accepte une course - Passage direct en statut 'acceptée'"""
    
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    
    # Vérifier que la course existe et est en attente
    ride = supabase.table("rides").select("*").eq("id", ride_id).eq("status", "en attente").execute()
    if not ride.data:
        raise HTTPException(status_code=404, detail="Course non trouvée ou déjà prise")
    
    # ✅ CHANGEMENT : On passe directement à "acceptée" et on assigne le driver_id
    supabase.table("rides").update({
        "driver_id": driver_id,
        "status": "acceptée", # <--- Changé de "matching" à "acceptée"
        "matched_at": "now()"
    }).eq("id", ride_id).execute()
    
    return {"message": "Course acceptée", "ride_id": ride_id, "status": "acceptée"}
    
@app.get("/ride/matching-status/{ride_id}")
def get_matching_status(ride_id: str, current_user = Depends(get_current_user)):
    ride = supabase.table("rides").select("*").eq("id", ride_id).execute()
    if not ride.data:
        raise HTTPException(status_code=404, detail="Course non trouvée")
    
    return {"ride": ride.data[0]}

@app.get("/driver/active-rides")
def get_active_rides(current_user = Depends(get_current_user)):
    """Récupère les courses ACTIVES du chauffeur (non terminées)"""
    
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    
    # ✅ Exclure les courses terminées et annulées
    result = supabase.table("rides")\
        .select("*, client:client_id(id, email, full_name)")\
        .eq("driver_id", driver_id)\
        .in_("status", ["matching", "acceptée", "picked_up", "en cours"])\
        .order("created_at", desc=True)\
        .execute()
    
    formatted = []
    for ride in result.data:
        client = ride.get("client", {})
        formatted.append({
            "id": ride["id"],
            "client_name": client.get("full_name", client.get("email", "Client")),
            "destination": ride["destination"],
            "status": ride["status"],
            "offer": ride.get("offer", 30)
        })
    
    return {"rides": formatted}

# ========== SUIVI DE COURSE ==========

@app.put("/ride/pickup/{ride_id}")
def driver_arrived(ride_id: str, current_user = Depends(get_current_user)):
    """Chauffeur signale qu'il a pris le client en charge"""
    
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    
    # Vérifier que la course existe
    ride = supabase.table("rides").select("*").eq("id", ride_id).eq("driver_id", driver_id).execute()
    
    if not ride.data:
        raise HTTPException(status_code=404, detail="Course non trouvée")
    
    ride_data = ride.data[0]
    current_status = ride_data.get("status")
    
    print(f"📌 Pickup - Statut actuel: {current_status}")
    
    # Si déjà picked_up, retourner succès
    if current_status == "picked_up":
        return {"message": "Client déjà pris en charge", "status": "picked_up"}
    
    # ✅ Changer le statut de "acceptée" à "picked_up"
    if current_status in ["acceptée", "accepted"]:
        supabase.table("rides").update({
            "status": "picked_up",
            "picked_up_at": "now()"
        }).eq("id", ride_id).execute()
        
        print(f"✅ Course {ride_id}: 'acceptée' -> 'picked_up'")
        
        return {"message": "Client pris en charge", "status": "picked_up"}
    else:
        raise HTTPException(status_code=400, detail=f"Course non acceptée. Statut: {current_status}")

@app.put("/ride/start/{ride_id}")
def start_ride(ride_id: str, current_user = Depends(get_current_user)):
    """Chauffeur démarre la course"""
    
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    
    ride = supabase.table("rides").select("*").eq("id", ride_id).eq("driver_id", driver_id).execute()
    
    if not ride.data:
        raise HTTPException(status_code=404, detail="Course non trouvée")
    
    ride_data = ride.data[0]
    current_status = ride_data.get("status")
    
    print(f"📌 Start - Statut actuel: {current_status}")
    
    # ✅ Changer le statut de "picked_up" à "en cours"
    if current_status == "picked_up":
        supabase.table("rides").update({
            "status": "en cours",
            "started_at": "now()"
        }).eq("id", ride_id).execute()
        
        print(f"✅ Course {ride_id}: 'picked_up' -> 'en cours'")
        
        return {"message": "Course démarrée", "status": "en cours"}
    else:
        raise HTTPException(status_code=400, detail=f"Course non prête à démarrer. Statut: {current_status}")

@app.put("/ride/complete/{ride_id}")
def complete_ride(ride_id: str, current_user = Depends(get_current_user)):
    """Terminer la course (chauffeur ou client)"""
    
    user_id = current_user.get("sub")
    role = current_user.get("role")
    
    # Récupérer la course
    ride = supabase.table("rides").select("*").eq("id", ride_id).execute()
    
    if not ride.data:
        raise HTTPException(status_code=404, detail="Course non trouvée")
    
    ride_data = ride.data[0]
    current_status = ride_data.get("status")
    
    print(f"Complete - Statut actuel: {current_status}, role: {role}, user: {user_id}")
    
    # ✅ Vérifier les droits (chauffeur OU client assigné)
    if role == "driver" and ride_data.get("driver_id") != user_id:
        raise HTTPException(status_code=403, detail="Vous n'êtes pas le chauffeur de cette course")
    
    if role == "client" and ride_data.get("client_id") != user_id:
        raise HTTPException(status_code=403, detail="Vous n'êtes pas le client de cette course")
    
    # ✅ Vérifier que la course est en cours ou acceptée
    if current_status not in ["en cours", "acceptée", "accepted", "picked_up"]:
        raise HTTPException(status_code=400, detail=f"Course non en cours. Statut: {current_status}")
    
    # Terminer la course
    supabase.table("rides").update({
        "status": "terminée",
        "completed_at": "now()"
    }).eq("id", ride_id).execute()
    
    print(f"✅ Course {ride_id} terminée par {role}")
    
    return {"message": "Course terminée", "status": "terminée"}

@app.delete("/ride/cancel/{ride_id}")
def cancel_ride(ride_id: str, current_user = Depends(get_current_user)):
    """Annuler une course (chauffeur ou client)"""
    
    user_id = current_user.get("sub")
    role = current_user.get("role")
    
    ride = supabase.table("rides").select("*").eq("id", ride_id).execute()
    
    if not ride.data:
        raise HTTPException(status_code=404, detail="Course non trouvée")
    
    ride_data = ride.data[0]
    current_status = ride_data.get("status")
    
    # ✅ Vérifier les droits
    if role == "client" and ride_data["client_id"] != user_id:
        raise HTTPException(status_code=403, detail="Non autorisé")
    elif role == "driver" and ride_data.get("driver_id") != user_id:
        raise HTTPException(status_code=403, detail="Non autorisé")
    
    # ✅ Annuler uniquement si pas encore terminée
    if current_status in ["terminée"]:
        raise HTTPException(status_code=400, detail="Course déjà terminée")
    
    supabase.table("rides")\
        .update({"status": "annulée", "cancelled_at": "now()"})\
        .eq("id", ride_id)\
        .execute()
    
    return {"message": "Course annulée", "ride_id": ride_id}

# ========== SUGGESTIONS DE LIEUX ==========

@app.get("/places/search")
def search_places(q: str, limit: int = 10, current_user = Depends(get_current_user)):
    if not q or len(q) < 2:
        return {"suggestions": []}
    
    result = supabase.table("places")\
        .select("name, address, latitude, longitude, category")\
        .ilike("name", f"%{q}%")\
        .limit(limit)\
        .execute()
    
    suggestions = []
    for place in result.data:
        suggestions.append({
            "name": place["name"],
            "address": place["address"],
            "latitude": place["latitude"],
            "longitude": place["longitude"],
            "description": f"{place['name']} - {place['address']}"
        })
    
    return {"suggestions": suggestions}

@app.get("/places/nearby")
def get_nearby_places(lat: float, lng: float, radius_km: float = 5, current_user = Depends(get_current_user)):
    result = supabase.table("places")\
        .select("name, address, latitude, longitude")\
        .execute()
    
    nearby = []
    for place in result.data:
        distance = calculate_distance(lat, lng, place["latitude"], place["longitude"])
        if distance <= radius_km:
            nearby.append({
                "name": place["name"],
                "address": place["address"],
                "latitude": place["latitude"],
                "longitude": place["longitude"],
                "distance_km": round(distance, 2)
            })
    
    nearby.sort(key=lambda x: x["distance_km"])
    return {"places": nearby[:10]}

# ========== GESTION DES COURSES (CLIENT) ==========

@app.post("/ride/request")
def request_ride(request_data: dict, current_user = Depends(get_current_user)):
    """Client demande une course avec choix du véhicule et prix proposé"""
    
    if current_user.get("role") != "client":
        raise HTTPException(status_code=403, detail="Seuls les clients peuvent demander une course")
    
    user_id = current_user.get("sub")
    
    # Récupérer les données
    origin_lat = request_data.get("origin_lat")
    origin_lng = request_data.get("origin_lng")
    origin_address = request_data.get("origin_address", "Position actuelle")
    destination = request_data.get("destination")
    destination_lat = request_data.get("destination_lat")
    destination_lng = request_data.get("destination_lng")
    vehicle_type = request_data.get("vehicle_type", "voiture")
    offer = request_data.get("offer", 30)  # ✅ Prix proposé par le client
    
    # Vérifications
    if not destination:
        raise HTTPException(status_code=400, detail="Destination requise")
    
    if not destination_lat or not destination_lng:
        raise HTTPException(status_code=400, detail="Coordonnées de destination requises")
    
    # Vérifier si le client a déjà une course en cours
    existing_ride = supabase.table("rides")\
        .select("*")\
        .eq("client_id", user_id)\
        .in_("status", ["en attente", "acceptée", "en cours"])\
        .execute()
    
    if existing_ride.data:
        raise HTTPException(status_code=400, detail="Vous avez déjà une course en cours")
    
    # Utiliser la géolocalisation du client pour l'adresse de départ
    if origin_lat is not None and origin_lng is not None:
        origin_address = get_address_from_coordinates(origin_lat, origin_lng)
    
    ride_data = {
        "client_id": user_id,
        "origin_lat": origin_lat,
        "origin_lng": origin_lng,
        "origin_address": origin_address,
        "destination": destination,
        "destination_lat": destination_lat,
        "destination_lng": destination_lng,
        "vehicle_type": vehicle_type,
        "offer": offer,
        "status": "en attente"
    }
    
    # Créer la course
    try:
        new_ride = supabase.table("rides").insert(ride_data).execute()
        
        return {
            "message": "Course demandée avec succès",
            "ride_id": new_ride.data[0]["id"],
            "status": "en attente",
            "vehicle_type": vehicle_type,
            "offer": offer
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erreur: {str(e)}")


@app.get("/client/stats")
def get_client_stats(current_user = Depends(get_current_user)):
    """Récupère les statistiques du client (historique)"""
    
    if current_user.get("role") != "client":
        raise HTTPException(status_code=403, detail="Réservé aux clients")
    
    user_id = current_user.get("sub")
    
    # Total des courses terminées
    completed_rides = supabase.table("rides")\
        .select("id, offer, created_at")\
        .eq("client_id", user_id)\
        .eq("status", "terminée")\
        .execute()
    
    total_trips = len(completed_rides.data)
    total_spent = sum(r.get("offer", 0) for r in completed_rides.data)
    
    # Courses annulées
    cancelled_rides = supabase.table("rides")\
        .select("id")\
        .eq("client_id", user_id)\
        .eq("status", "annulée")\
        .execute()
    
    cancelled_trips = len(cancelled_rides.data)
    
    return {
        "total_trips": total_trips,
        "total_spent": total_spent,
        "cancelled_trips": cancelled_trips
    }


@app.get("/client/ride-history")
def get_client_ride_history(current_user = Depends(get_current_user)):
    """Récupère l'historique des courses du client"""
    
    if current_user.get("role") != "client":
        raise HTTPException(status_code=403, detail="Réservé aux clients")
    
    user_id = current_user.get("sub")
    
    result = supabase.table("rides")\
        .select("*, driver:driver_id(id, email, full_name)")\
        .eq("client_id", user_id)\
        .order("created_at", desc=True)\
        .limit(50)\
        .execute()
    
    formatted = []
    for ride in result.data:
        driver_info = ride.get("driver", {})
        # ✅ Vérifier si driver_info n'est pas None
        driver_name = ""
        if driver_info is not None:
            driver_name = driver_info.get("full_name", "")
        
        formatted.append({
            "id": ride["id"],
            "destination": ride["destination"],
            "offer": ride.get("offer", 0),
            "status": ride["status"],
            "vehicle_type": ride.get("vehicle_type", "voiture"),
            "created_at": ride["created_at"],
            "driver_name": driver_name
        })
    
    return {"rides": formatted}

@app.get("/client/profile")
def get_client_profile(current_user = Depends(get_current_user)):
    """Récupère le profil du client"""
    
    if current_user.get("role") != "client":
        raise HTTPException(status_code=403, detail="Réservé aux clients")
    
    user_id = current_user.get("sub")
    
    result = supabase.table("users")\
        .select("id, email, full_name, tel, created_at")\
        .eq("id", user_id)\
        .execute()
    
    if not result.data:
        raise HTTPException(status_code=404, detail="Utilisateur non trouvé")
    
    user_data = result.data[0]
    
    # Compter les courses terminées
    completed_rides = supabase.table("rides")\
        .select("id")\
        .eq("client_id", user_id)\
        .eq("status", "terminée")\
        .execute()
    
    return {
        "id": user_data["id"],
        "email": user_data["email"],
        "full_name": user_data.get("full_name", ""),
        "tel": user_data.get("tel", ""),
        "member_since": user_data.get("created_at", ""),
        "total_rides": len(completed_rides.data)
    }


@app.put("/client/profile")
def update_client_profile(request_data: dict, current_user = Depends(get_current_user)):
    """Met à jour le profil du client"""
    
    if current_user.get("role") != "client":
        raise HTTPException(status_code=403, detail="Réservé aux clients")
    
    user_id = current_user.get("sub")
    
    update_data = {}
    if "full_name" in request_data:
        update_data["full_name"] = request_data["full_name"]
    if "tel" in request_data:
        update_data["tel"] = request_data["tel"]
    
    if update_data:
        supabase.table("users").update(update_data).eq("id", user_id).execute()
    
    return {"message": "Profil mis à jour"}


# ========== FONCTION DE GÉOCODAGE ==========

def get_address_from_coordinates(lat, lng):
    """Convertir des coordonnées en adresse (utilisation de Nominatim OpenStreetMap)"""
    try:
        import requests
        url = f"https://nominatim.openstreetmap.org/reverse?lat={lat}&lon={lng}&format=json"
        response = requests.get(url, headers={'User-Agent': 'RideNow/1.0'})
        if response.status_code == 200:
            data = response.json()
            return data.get('display_name', 'Position actuelle')
    except:
        pass
    return f"Position à {lat}, {lng}"

@app.get("/ride/current")
def get_current_ride(current_user = Depends(get_current_user)):
    """Récupère la course en cours de l'utilisateur"""
    
    user_id = current_user.get("sub")
    role = current_user.get("role")
    
    print(f"🔍 get_current_ride - user: {user_id}, role: {role}")
    
    if role == "driver":
        result = supabase.table("rides")\
            .select("*, client:client_id(id, email, full_name, tel)")\
            .eq("driver_id", user_id)\
            .in_("status", ["acceptée", "picked_up", "en cours"])\
            .order("created_at", desc=True)\
            .limit(1)\
            .execute()
    else:
        result = supabase.table("rides")\
            .select("*, driver:driver_id(id, email, drivers(full_name, phone, vehicle_type, rating))")\
            .eq("client_id", user_id)\
            .in_("status", ["en attente", "acceptée", "en cours"])\
            .order("created_at", desc=True)\
            .limit(1)\
            .execute()
    
    if not result.data:
        print("❌ Aucune course trouvée")
        return {"ride": None}
    
    ride_data = result.data[0]
    print(f"✅ Course trouvée - status: {ride_data.get('status')}")
    
    return {"ride": ride_data}

# ========== NÉGOCIATION ==========

@app.post("/ride/negotiate/{ride_id}")
def negotiate_ride(ride_id: str, request_data: dict, current_user = Depends(get_current_user)):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    counter_offer = request_data.get("counter_offer")
    
    if not counter_offer:
        raise HTTPException(status_code=400, detail="Montant requis")
    
    ride = supabase.table("rides").select("*").eq("id", ride_id).eq("status", "en attente").execute()
    if not ride.data:
        raise HTTPException(status_code=404, detail="Course non trouvée")
    
    ride_data = ride.data[0]
    
    existing_neg = supabase.table("negotiations").select("*").eq("ride_id", ride_id).eq("driver_id", driver_id).execute()
    
    if existing_neg.data:
        supabase.table("negotiations").update({
            "counter_offer": counter_offer,
            "status": "pending",
            "updated_at": "now()"
        }).eq("ride_id", ride_id).eq("driver_id", driver_id).execute()
    else:
        supabase.table("negotiations").insert({
            "ride_id": ride_id,
            "driver_id": driver_id,
            "client_id": ride_data["client_id"],
            "original_offer": ride_data.get("offer", 0),
            "counter_offer": counter_offer,
            "status": "pending"
        }).execute()
    
    return {"message": "Contre-offre envoyée", "counter_offer": counter_offer}

@app.post("/ride/decline/{ride_id}")
def decline_ride(ride_id: str, current_user = Depends(get_current_user)):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    
    supabase.table("ride_declines").insert({
        "ride_id": ride_id,
        "driver_id": driver_id,
        "declined_at": "now()"
    }).execute()
    
    return {"message": "Course refusée"}

# ========== NOTIFICATIONS ==========

@app.get("/notifications")
def get_notifications(current_user = Depends(get_current_user)):
    user_id = current_user.get("sub")
    
    result = supabase.table("notifications")\
        .select("*")\
        .eq("user_id", user_id)\
        .order("created_at", desc=True)\
        .limit(50)\
        .execute()
    
    return {"notifications": result.data}

@app.put("/notifications/read/{notification_id}")
def mark_notification_read(notification_id: str, current_user = Depends(get_current_user)):
    supabase.table("notifications").update({"is_read": True}).eq("id", notification_id).execute()
    
    return {"message": "Notification marquée comme lue"}

# ========== STATISTIQUES CHAUFFEUR ==========

@app.get("/driver/stats")
def get_driver_stats(current_user = Depends(get_current_user)):
    if current_user.get("role") != "driver":
        raise HTTPException(status_code=403, detail="Réservé aux chauffeurs")
    
    driver_id = current_user.get("sub")
    today = datetime.now().date()
    
    today_rides = supabase.table("rides")\
        .select("id, offer")\
        .eq("driver_id", driver_id)\
        .eq("status", "terminée")\
        .gte("completed_at", f"{today}T00:00:00")\
        .lt("completed_at", f"{today}T23:59:59")\
        .execute()
    
    today_trips = len(today_rides.data)
    today_earnings = sum(r.get("offer", 0) for r in today_rides.data)
    
    all_rides = supabase.table("rides")\
        .select("status")\
        .eq("driver_id", driver_id)\
        .gte("created_at", (datetime.now() - timedelta(days=30)).isoformat())\
        .execute()
    
    total = len(all_rides.data)
    accepted = sum(1 for r in all_rides.data if r["status"] in ["terminée", "en cours", "acceptée"])
    accept_rate = int((accepted / total) * 100) if total > 0 else 0
    
    total_rides = supabase.table("rides")\
        .select("id")\
        .eq("driver_id", driver_id)\
        .execute()
    
    return {
        "today_trips": today_trips,
        "today_earnings": today_earnings,
        "accept_rate": accept_rate,
        "total_trips": len(total_rides.data)
    }

# ========== HISTORIQUE ==========

@app.get("/rides/history")
def get_ride_history(current_user = Depends(get_current_user)):
    user_id = current_user.get("sub")
    role = current_user.get("role")
    
    if role == "client":
        result = supabase.table("rides")\
            .select("*")\
            .eq("client_id", user_id)\
            .in_("status", ["terminée", "annulée"])\
            .order("created_at", desc=True)\
            .limit(50)\
            .execute()
    else:
        result = supabase.table("rides")\
            .select("*")\
            .eq("driver_id", user_id)\
            .in_("status", ["terminée", "annulée"])\
            .order("created_at", desc=True)\
            .limit(50)\
            .execute()
    
    return {"rides": result.data}