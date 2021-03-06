import json
import urllib.request
import time

import secret_params

from numpy import mean


def get_geocode_json(query_str):
    try:
        q = '&q=' + query_str
        polygon = '&polygon=0'
        bounded = '&bounded=1'
        url = 'https://nominatim.openstreetmap.org/search?format=json' + q + polygon + bounded
        print(url)
        req = urllib.request.Request(url)
        req.add_header('User-Agent', 'Mozilla/7.0')
        response = urllib.request.urlopen(req)
        return json.loads(response.read().decode(response.info().get_param('charset') or 'utf-8'))
    except urllib.request.HTTPError as http_error:
        print(http_error)
        return []
    finally:
        time.sleep(0.85)


def get_reverse_geocode_json(lat, lon):
    try:
        lat = '&lat=' + lat
        lon = '&lon=' + lon
        url = 'https://nominatim.openstreetmap.org/reverse?format=json' + lat + lon
        #print(url)
        req = urllib.request.Request(url)
        req.add_header('User-Agent', 'Mozilla/7.0')
        req.add_header('Connection', 'close')
        response = urllib.request.urlopen(req)
        return json.loads(response.read().decode(response.info().get_param('charset') or 'utf-8'))
    except urllib.request.HTTPError as http_error:
        print(http_error)
        return []
    finally:
        time.sleep(0.81)


def get_reverse_geocode_json_google(lat, lon):
    try:
        key = '&key=' + secret_params.google_geo_api_key
        result_type_constraint = '&result_type=administrative_area_level_1'
        lat_lon = '&latlng=' + lat + ',' + lon
        url = 'https://maps.googleapis.com/maps/api/geocode/json?' + lat_lon + result_type_constraint + key
        req = urllib.request.Request(url)
        req.add_header('User-Agent', 'Mozilla/7.0')
        response = urllib.request.urlopen(req)
        return json.loads(response.read().decode(response.info().get_param('charset') or 'utf-8'))
    except urllib.request.HTTPError as http_error:
        print(http_error)
        return []
    finally:
        time.sleep(0.02)

# data = get_geocode_json('izmir')
# box = [float(i) for i in data[0]['boundingbox']]
# print(box)
# avg = [mean(box[0:1]), mean(box[2:3])]
# print(avg)
