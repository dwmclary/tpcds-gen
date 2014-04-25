#! /bin/awk -f

BEGIN {FS="|"}
{
#main loop -- web_sales and catalog_sales have 35 pipes
    if (NF == 35){print $0}
}