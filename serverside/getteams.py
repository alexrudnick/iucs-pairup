#!/usr/bin/env python

import cgi
import cgitb
import json
import sys
import random
import os

def listteams(usernames):
  out = []

  if len(usernames) % 2 == 1:
    out.append(usernames[:3])
    usernames = usernames[3:]

  assert (len(usernames) % 2 == 0)
  lefts = usernames[::2]
  rights = usernames[1::2]

  # Put the two lists together.
  for (left,right) in zip(lefts,rights):
    out.append( [left, right] )
  return out

def conflicts_trio(usernames):
  """Count the number of conflicts for a proposed team of three."""
  assert len(usernames) == 3

  out = 0
  if havePaired(usernames[0], usernames[1]): out += 1
  if havePaired(usernames[0], usernames[2]): out += 1
  if havePaired(usernames[1], usernames[2]): out += 1
  return out

def count_conflicts(usernames):
  if len(usernames) % 2 == 1:
    return conflicts_trio(usernames[:3]) + count_conflicts(usernames[3:])

  assert (len(usernames) % 2 == 0)
  # every other username. Then the rest of them go into "right".
  lefts = usernames[::2]
  rights = usernames[1::2]

  # Put the two lists together.
  nconflicts = 0
  for (left,right) in zip(lefts,rights):
    if havePaired(left,right):
      nconflicts += 1
  return nconflicts

def bogoorder(usernames):
  MAXTRIES = 100000
  tries = 0
  bestscore = sys.maxint
  best = usernames

  while tries < MAXTRIES:
    proposed = usernames[:]
    random.shuffle(proposed)

    score = count_conflicts(proposed)
    if score == 0:
      return proposed

    if score < bestscore:
      bestscore = score
      best = proposed
    tries += 1

  return best

def makeKey(username1, username2):
  """Put the two usernames in alpha order and return a tuple."""
  return (min(username1, username2), max(username1, username2))

def havePaired(username1, username2):
  global PASTPAIRS
  key = makeKey(username1, username2)
  return key in PASTPAIRS

def load_past_pairs(fn):
  """Load up the pairs that have been formed in the past."""
  pastpairs = {}

  with open(fn, "r") as f:
    lines = f.readlines()

    for line in lines:
      if line.startswith("#"):
        continue
      left, right = (line.strip()).split()
      key = (left,right)
      if key in pastpairs:
        pastpairs[key] += 1
      else:
        pastpairs[key] = 1
  return pastpairs

def main():
  global PASTPAIRS

  print "Content-type: application/json\n"

  text = sys.stdin.read()
  # the first thing in the list is the section number.
  thelist = json.loads(text)
  section = thelist[0]
  usernames = thelist[1:]

  pastpairsfn = section + ".pastpairs"
  PASTPAIRS = load_past_pairs(pastpairsfn)

  ordering = bogoorder(usernames)
  teams = listteams(ordering)
  out = [section] + teams
  print json.dumps(out)

if __name__ == "__main__": main()
