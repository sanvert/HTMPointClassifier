import os  # for manipulates files and subdirectories
import json  # handle json files
import geocoder


def process_geolocation_files(json_input_folder, json_output_folder):
    # In order to get the list of all files that ends with ".json"
    # we will get list of all files, and take only the ones that ends with "json"
    json_files = [x for x in os.listdir(json_input_folder) if x.endswith("json")]
    for json_file in json_files:
        json_data = ''
        json_file_path = os.path.join(json_input_folder, json_file)
        base_file_name = os.path.basename(json_file_path)
        json_file_region = base_file_name.split("_")[0].lower()
        with open(json_file_path, "r") as f:
            print(json_file_region)
            lines = f.readlines()
            for line in lines:
                json_line = json.loads(line)
                coordinates = [str(coordinate) for coordinate in json_line["coordinates"]]
                reverse_geocode = geocoder.get_reverse_geocode_json(coordinates[0], coordinates[1])
                # print(reverse_geocode)
                if str(reverse_geocode).lower().find(json_file_region) >= 0:
                    json_data += str(json_line) + '\n'
                    # json_data.append(json_line)
                else:
                    print(json_line)
        # print(json_data)
        output_path = os.path.join(json_output_folder, base_file_name)
        with open(output_path, "w") as f:
            #    json.dump(json_data, f)
            f.write(json_data)


input_folder = os.path.join("data/temp/")
output_folder = os.path.join("data/processed")

process_geolocation_files(input_folder, output_folder)
