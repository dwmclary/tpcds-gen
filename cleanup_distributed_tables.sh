#! /bin/bash

########################### WEB #######################
#cleanup empty web returns
hdfs dfs -rm -r $1/web_returns

#separate the web returns
hadoop jar /usr/lib/hadoop-mapreduce/hadoop-streaming.jar -D mapred.reduce.tasks=0 -input $1/web_sales -output $1/web_returns -mapper web_returns.awk -reducer /bin/cat -file ./web_returns.awk

#separate the web sales
hadoop jar /usr/lib/hadoop-mapreduce/hadoop-streaming.jar -D mapred.reduce.tasks=0 -input $1/web_sales -output $1/web_sales_clean -mapper web_sales.awk -reducer /bin/cat -file ./web_sales.awk

#remove the old web sales directory
hdfs dfs -rm -r $1/web_sales
hdfs dfs -mv $1/web_sales_clean $1/web_sales

########################### CATALOGS  #######################
#cleanup empty catalog returns
hdfs dfs -rm -r $1/catalog_returns

#separate the catalog returns
hadoop jar /usr/lib/hadoop-mapreduce/hadoop-streaming.jar -D mapred.reduce.tasks=0 -input $1/catalog_sales -output $1/catalog_returns -mapper catalog_returns.awk -reducer /bin/cat -file ./catalog_returns.awk

#separate the catalog sales
hadoop jar /usr/lib/hadoop-mapreduce/hadoop-streaming.jar -D mapred.reduce.tasks=0 -input $1/catalog_sales -output $1/catalog_sales_clean -mapper web_sales.awk -reducer /bin/cat -file ./web_sales.awk

#remove the old catalog sales directory
hdfs dfs -rm -r $1/catalog_sales
hdfs dfs -mv $1/catalog_sales_clean $1/catalog_sales

########################### STORES #######################
#cleanup empty store returns
hdfs dfs -rm -r $1/store_returns

#separate the store returns
hadoop jar /usr/lib/hadoop-mapreduce/hadoop-streaming.jar -D mapred.reduce.tasks=0 -input $1/store_sales -output $1/store_returns -mapper store_returns.awk -reducer /bin/cat -file ./store_returns.awk

#separate the store sales
hadoop jar /usr/lib/hadoop-mapreduce/hadoop-streaming.jar -D mapred.reduce.tasks=0 -input $1/store_sales -output $1/store_sales_clean -mapper store_sales.awk -reducer /bin/cat -file ./store_sales.awk

#remove the old store sales directory
hdfs dfs -rm -r $1/store_sales
hdfs dfs -mv $1/store_sales_clean $1/store_sales