# To extract HTM pairs for a specific region run Spherical.Converter dll

dotnet Spherical.Converter.dll <input GeoJSON formatted region file path> <region_name> <region_id>

#Example Usage:
dotnet Spherical.Converter.dll "/Users/sanvertarmur/Desktop/DEV/istanbul_geo.json" Istanbul 1
dotnet Spherical.Converter.dll "/Users/sanvertarmur/Desktop/DEV/ankara_geo.json" Ankara 2
dotnet Spherical.Converter.dll "/Users/sanvertarmur/Desktop/DEV/izmir_geo.json" Izmir 3

#OpenStreetMap Links
#Istanbul
http://polygons.openstreetmap.fr/index.py?id=223474
#Ankara
http://polygons.openstreetmap.fr/index.py?id=223422
#Izmir
http://polygons.openstreetmap.fr/index.py?id=223167
