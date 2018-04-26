import tweepy
import json
import sys
import datetime
import atexit

from numpy import mean

import secret_params

consumer_key = secret_params.consumer_key
consumer_secret = secret_params.consumer_secret
access_token = secret_params.access_token
access_token_secret = secret_params.access_token_secret

# Generated with http://boundingbox.klokantech.com/
GEOBOX_ISTANBUL = [28.146472, 40.736531, 29.316821, 41.583175,
                   29.316821, 40.948929, 29.498401, 41.407169,
                   29.498401, 41.045667, 29.8875, 41.407169]

GEOBOX_IZMIR = [26.2342, 38.0351, 28.368, 38.367,
                26.8757, 38.957, 27.4494, 39.3309,
                26.4149, 38.3971, 27.1503, 38.9261]

GEOBOX_ANKARA = [31.9586, 39.3283, 33.3868, 40.324,
                 31.04, 40.0072, 31.9586, 40.308,
                 32.2241, 40.308, 32.8742, 40.6261]

auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_token_secret)


# api = tweepy.API(auth)


class LocationTweetListener(tweepy.StreamListener):
    def __init__(self, geobox, name):
        self.geo_name = name
        self.geo_avg = [mean(geobox[1::2]), mean(geobox[0::2])]
        self.filename = name + "_" + datetime.datetime.now().strftime("%Y-%m-%d") + ".json"
        self.file = open('data/' + self.filename, mode='a', encoding='utf-8')
        self.num_of_recs = 0
        atexit.register(self.on_exit)

    def on_data(self, data):
        try:
            eligible = None
            json_data = json.loads(data)

            output_json = dict()
            output_json['id'] = json_data['id']
            output_json['text'] = json_data['text']
            output_json['created_at'] = json_data['created_at']

            if json_data['geo']:
                eligible = True
                output_json['coordinates'] = self.convert_geo_element(json_data['geo'])

            if json_data['place']:
                output_json['city'] = json_data['place']['full_name']
                if not eligible and json_data['place']['bounding_box']:
                    output_json['coordinates'] \
                        = list(self.convert_geo_element(json_data['place']['bounding_box'])[::-1])
                elif not eligible:
                    output_json['coordinates'] = self.geo_avg
                eligible = True

            if eligible and self.check_if_near_geo_box(output_json['coordinates']):
                self.file.write(json.dumps(output_json) + "\n")
                self.num_of_recs = self.num_of_recs + 1
                if self.num_of_recs % 10 == 0:
                    print(str(self.num_of_recs) + " records are processed for " + self.geo_name)
                    self.file.flush()

        except Exception as e:
            print(e)

        return True

    def on_error(self, status_code):
        print('Encountered error with status code:' + str(status_code), file=sys.stderr)
        return True  # Don't kill the stream

    def on_timeout(self):
        print('Timeout...', file=sys.stderr)
        return True  # Don't kill the stream

    def on_disconnect(self):
        self.file.close()
        print('Disconnect...', file=sys.stderr)
        return False  # Kill the stream

    def check_if_near_geo_box(self, point):
        return abs(point[0] - self.geo_avg[0]) <= 1.5 and abs(point[1] - self.geo_avg[1])

    def on_exit(self):
        self.file.flush()
        self.file.close()
        print("Goodbye! " + self.geo_name)

    @staticmethod
    def convert_geo_element(geo):
        if geo['type'] == 'Point':
            return geo['coordinates']
        else:
            return mean(geo['coordinates'][0], axis=0)


tweepy.Stream(auth=auth, listener=LocationTweetListener(GEOBOX_ISTANBUL, "Istanbul"))\
    .filter(locations=GEOBOX_ISTANBUL, async=True)

tweepy.Stream(auth=auth, listener=LocationTweetListener(GEOBOX_IZMIR, "Izmir"))\
    .filter(locations=GEOBOX_IZMIR, async=True)

tweepy.Stream(auth=auth, listener=LocationTweetListener(GEOBOX_ANKARA, "Ankara")) \
    .filter(locations=GEOBOX_ANKARA, async=True)
