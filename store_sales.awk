#! /bin/awk -f

BEGIN {FS="|"}
{
#main loop -- store_sales has 24 pipes
    if (NF == 24){print $0}
}