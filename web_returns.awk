#! /bin/awk -f

BEGIN {FS="|"}
{
    if (NF == 25){print $0}
}