import tweepy
import json
import sys
import datetime

from numpy import mean

consumer_key = 'z9EIrCKZneJPaPvwKX3vCWV6u'
consumer_secret = 'FHYsnKFagSnGiYanBwgyGRWYcTStsFBrs3igUkfisewnHINiEu'
access_token = '761188289890115584-GSQp1Xpwe420QWn5RlDK0SwPCU9FazT'
access_token_secret = '21ZaFNcfg68X358y6NPOgC97acHCqq4mXpu2xgUHvmfXV'

# Generated with http://boundingbox.klokantech.com/
GEOBOX_ISTANBUL = [28.557086, 40.815609, 29.383157, 41.37483]
GEOBOX_IZMIR = []
GEOBOX_ANKARA = []

auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_token_secret)


def read_file_content(filename):
    with open(filename) as f:
        lines = f.readlines()
        for line in lines:
            print(json.loads(line)['id'])

# api = tweepy.API(auth)


class LocationTweetListener(tweepy.StreamListener):
    def __init__(self, geobox, name):
        self.geo_avg = [mean([geobox[1], geobox[3]]), mean([geobox[0], geobox[2]])]
        self.filename = name + "_" + datetime.datetime.now().strftime("%Y-%m-%d") + ".json"
        self.file = open(self.filename, mode='a', encoding='utf-8')
        self.num_of_recs = 0;

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
                print("geo")

            if json_data['place']:
                output_json['city'] = json_data['place']['full_name']
                if not eligible and json_data['place']['bounding_box']:
                    output_json['coordinates'] \
                        = list(self.convert_geo_element(json_data['place']['bounding_box'])[::-1])
                    print("reverse place")
                elif not eligible:
                    print("reverse geobox")
                    output_json['coordinates'] = self.geo_avg
                eligible = True

            if eligible and self.check_if_near_geo_box(output_json['coordinates']):
                print("RES")
                print(output_json)
                self.file.write(json.dumps(output_json) + "\n")
                self.num_of_recs = self.num_of_recs + 1;
                if self.num_of_recs % 10 == 0:
                    self.file.flush()

        except Exception as e:
            print(e)

        return True

    def on_error(self, status_code):
        print >> sys.stderr, 'Encountered error with status code:', status_code
        return True  # Don't kill the stream

    def on_timeout(self):
        print >> sys.stderr, 'Timeout...'
        return True  # Don't kill the stream

    def on_disconnect(self):
        self.file.close()
        print >> sys.stderr, 'Disconnect...'
        return False  # Kill the stream

    def check_if_near_geo_box(self, point):
        return abs(point[0] - self.geo_avg[0]) <= 1.5 and abs(point[1] - self.geo_avg[1])

    @staticmethod
    def convert_geo_element(self, geo):
        if geo['type'] == 'Point':
            return geo['coordinates']
        else:
            return mean(geo['coordinates'][0], axis=0)


locationTweetListener = LocationTweetListener(GEOBOX_ISTANBUL, "Istanbul")
locationStream = tweepy.Stream(auth=auth, listener=locationTweetListener)

locationStream.filter(locations=GEOBOX_ISTANBUL, async=True)
