#! /bin/awk -f

BEGIN {FS="|"}
{
#main loop -- catalog_returns has 28 pipes
    if (NF == 28){print $0}
}