import json
import os


def read_file_content(filename):
    with open(filename) as f:
        lines = f.readlines()
        for line in lines:
            print(json.loads(line)['id'])


def get_files(extension):
    return [f for f in os.listdir(os.getcwd()) if f.endswith(extension)]