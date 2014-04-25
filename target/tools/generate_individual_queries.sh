#! /bin/bash

for i in {1..99}; do
./dsqgen -verbose -input ../query_templates/templates.lst  -directory ../query_templates -dialect oracle -scale 2 -output_dir ../generated_queries -template ../query_templates/query$i.tpl -filter N > ../generated_queries/query_$i.sql
done