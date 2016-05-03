from collections import deque
from datetime import datetime

DATETIME_FORMAT_STRING = "%Y-%m-%d-%H:%M:%S"

# In reality, this would be replaced by some more durable storage, e.g.
# something like a MySQL or PostgreSQL
DATA_STORE = {
    "access_points": set(),
    "probes": deque(),
    "bitmaps": deque(),
    "maps": deque(),
    "readings": deque(),
}

def valid_map_id(id):
    return any(id == val["map_id"] for val in DATA_STORE["maps"])

#------------------------------------------------------------------------------

def access_points_get(map_id):
    return [
        {"mac_address": mac} for mac in DATA_STORE["access_points"]
    ]

#------------------------------------------------------------------------------

def probes_get(map_id, data_since=None):
    if not valid_map_id(map_id):
        # TODO replace with HTTP 400 in spec
        return []
    if data_since is not None:
        # assume utc
        after_now = datetime.strptime(data_since, DATETIME_FORMAT_STRING)
    
    ret = []
    for value in DATA_STORE["probes"]:
        date_okay = True
        if data_since is not None:
            if value["insert_dt"] < after_now:
                date_okay = False
                
        correct_map = value["map_id"] == map_id
        
        if date_okay and correct_map:
            ret.append(value["data"])

    return ret

def probes_post(data):
    succeeded, failed = 0, 0
    to_add = []
    
    for probe in data:
        # validate any attributes here
        # validate map_id
        if not valid_map_id(probe["map_id"]):
            failed += 1
            continue
        to_add.append(probe)
        succeeded += 1
     
    if succeeded > 0:
        current_dt = datetime.utcnow()
        map_id = to_add[0]["map_id"]
        insert_value = {
            "insert_dt": current_dt, "map_id": map_id, "data": to_add
        }
        DATA_STORE.get("probes").append(insert_value)
        
    return [succeeded, failed]

#------------------------------------------------------------------------------

def readings_get(map_id, data_since=None):
    if not valid_map_id(map_id):
        return []
    if data_since is not None:
        # assume utc
        after_now = datetime.strptime(data_since, DATETIME_FORMAT_STRING)
        
    ret = []
    for value in DATA_STORE["readings"]:
        date_okay = True
        if data_since is not None:
            if value["insert_dt"] < after_now:
                date_okay = False
        correct_map = value["map_id"] == map_id
        
        if date_okay and correct_map:
            ret.append(value["data"])                
    return ret

def readings_post(data):
    succeeded, failed = 0, 0
    to_add = []
    
    for reading in data:
        # validate any attributes here
        # validate map_id
        if not valid_map_id(reading["map_id"]):
            failed += 1
            continue
        DATA_STORE["access_points"].add(reading["mac_address"])
        to_add.append(reading)
        succeeded += 1
     
    if succeeded > 0:
        current_dt = datetime.utcnow()
        map_id = to_add[0]["map_id"]
        insert_value = {
            "insert_dt": current_dt, "map_id": map_id, "data": to_add
        }
        DATA_STORE["readings"].append(insert_value)
        
    return [succeeded, failed]

#------------------------------------------------------------------------------

def bitmap_get(id):
    ret = None
    
    # See if we have bitmap for this map id
    for val in DATA_STORE["bitmaps"]:
        if val["map_id"] == id:
            ret = val
            break

    # TODO change spec to return an HTTP 400 error here
    if ret is None:
        return {"map_id": 0, "data": ""}
    return ret

def bitmap_post(data):
    ids_with_bitmaps = set(val["map_id"] for val in DATA_STORE.get("bitmaps"))

    map_exists = valid_map_id(data["map_id"])
    bitmap_exists = data["map_id"] in ids_with_bitmaps
    
    print(map_exists, bitmap_exists)

    if not map_exists or bitmap_exists:
        return False
        
    # TODO should validate image and convert if necessary
    DATA_STORE["bitmaps"].append(data)
    return True

#------------------------------------------------------------------------------

def maps_all_get():
    return list(DATA_STORE["maps"])

def maps_get(id=None, name=None):
    if id is None and name is None:
        # TODO one of these should be defined
        id = 1
    
    for val in DATA_STORE["maps"]:
        if val["map_id"] == id or val["name"] == name:
            return val
    return {}

def maps_post(map_name):
    # TODO add verification step
    # TODO add better id
    id = len(DATA_STORE["maps"]) + 1
    value = {
        "map_id": id,
        "name": map_name,
    }
    
    DATA_STORE["maps"].append(value)
    return value

def maps_search_get(keywords):
    # TODO implement
    return []
