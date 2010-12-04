#!/usr/bin/env python

import cgi
import cgitb
import json
import glob

def load_rosters():
  out = {}
  for rosterfn in glob.glob("*.roster"):
    section = rosterfn.split('.')[0]
    out[section] = load_class(rosterfn)

  return out

def load_class(fn):
  with open(fn, "r") as f:
    lines = f.readlines()
    out = {}

    for line in lines:
      line = line.strip()
      if line.startswith("#") or len(line) == 0:
        continue
      username, realname = line.strip().split(None,1)
      out[username] = realname
    return out

print "Content-type: application/json\n"
# print """\
# {"2200" : {"achimendez":"Alberto Chimendez", "msiegel":"Martin Siegel"},
#  "2201" : {"aquizno":"Alice Quizno", "teconolodge":"Tallahassee Econolodge"}}
# """

print json.dumps(load_rosters())
