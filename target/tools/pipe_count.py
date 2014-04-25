#! /usr/bin/env python

import sys

counter = {}
for line in sys.stdin:
    try:
        count = len(line.strip().split("|"))
        if count in counter:
            counter[count] += 1
        else:
            counter[count] = 1
    except KeyboardInterrupt:
        break

for c in counter.keys():
    print c, counter[c], "times"
