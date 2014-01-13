#!/usr/bin/python
from google.appengine.api import oauth
from google.appengine.ext import db
from google.appengine.ext.webapp import template

import webapp2
import json
import urllib2
import logging

SERVER = 'https://android.googleapis.com/gcm/send'
PORT = 5235
USERNAME = "m4sterm1nd.research@gmail.com"
PASSWORD = "AIzaSyAPm-x0NAwuLuU8Hn6sbQ53c-zW3Dn3v8o"
BROWSER_PASSWORD = "AIzaSyDu3GRD3ja6YdyTvXJmqnsP3MfSIXZ7_oU"
regId = "APA91bHfvEgcB84TqzAApdXukQUg46QayRaayGnWTOaW_vlftFd7Cjk5oF4ataRTrGxeMy5Jjbqi6UsF1TPWVYb3-cxjwVmUGjgE0pzZDguQEbToC1FwYBSC7--7UPrS6PHhT4jlBi4ZBF5EbJUpx536JsOAcKVY9Q"

class MainHandler(webapp2.RequestHandler):
    def get(self):
        #user = oauth.get_current_user()
        user = 'corey.test'
        if user:
            values = { 'user': user }
            self.response.write(template.render('main.html', values))
        else:
            greeting = ('<a href="%s">Sign in or register</a>.' % oauth.create_login_url('/'))

class RegistrationHandler(webapp2.RequestHandler):
    def post(self):
        data = json.loads(self.request.body)
        registration_id = data.get('registration_id', None)
        client_id = data.get('client_id', None)
        logging.info('Received registration post: ' + str(registration_id)+ ', ' + str(client_id))
        self.redirect('/')

class EnableHandler(webapp2.RequestHandler):
    def post(self):
        doStateChange(1)
        self.redirect('/')
        
class DisableHandler(webapp2.RequestHandler):
    def post(self):
        doStateChange(0)
        self.redirect('/')

def doStateChange(state):
    registration_data = {
        "data"             : { "state" : state },
        "registration_ids" : [regId]
    }

    logging.info('Sending state: ' + str(state))

    headers = {
        'Content-Type'  : 'application/json',
        'Authorization' : 'key=' + BROWSER_PASSWORD
    }
    data = json.dumps(registration_data)
    req = urllib2.Request(SERVER, data, headers)

    f = urllib2.urlopen(req)
    response = f.read()
    f.close()

app = webapp2.WSGIApplication([('/', MainHandler),
    ('/enable', EnableHandler),
    ('/disable', DisableHandler),
    ('/registration_id_post', RegistrationHandler)],
    debug=True)
