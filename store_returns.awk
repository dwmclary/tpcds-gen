#! /bin/awk -f

BEGIN {FS="|"}
{
#main loop -- store_returns has 21 pipes
    if (NF == 21){print $0}
}