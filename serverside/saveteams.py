#!/usr/bin/env python

import cgi
import json
import sys
import time

def makeKey(username1, username2):
  """Put the two usernames in alpha order and return a tuple."""
  return (min(username1, username2), max(username1, username2))

def savePair(left, right, f):
  first,second = makeKey(left,right)
  f.write("%s %s\n" % (first, second))

def saveTeams(teams, fn):
  """Write out all of the teams."""
  with open(fn, "a+") as f:
    f.write("# " + time.asctime() + "\n")

    for team in teams:
      if len(team) == 3:
        savePair(team[0], team[1], f)
        savePair(team[0], team[2], f)
        savePair(team[1], team[2], f)
        continue

      assert len(team) == 2
      savePair(team[0], team[1], f)

def main():
  global PASTPAIRS

  print "Content-type: text/plain\n"

  text = sys.stdin.read()
  # the first thing in the list is the section number.
  thelist = json.loads(text)

  section = thelist[0]
  teams = thelist[1:]

  pastpairsfn = section + ".pastpairs"
  saveTeams(teams, pastpairsfn)

  print "OK"

if __name__ == "__main__": main()
