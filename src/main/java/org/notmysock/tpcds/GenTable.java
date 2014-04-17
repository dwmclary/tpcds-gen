package org.notmysock.tpcds;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.filecache.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.mapreduce.lib.reduce.*;

import org.apache.commons.cli.*;
import org.apache.commons.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.net.*;
import java.math.*;
import java.security.*;


public class GenTable extends Configured implements Tool {
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int res = ToolRunner.run(conf, new GenTable(), args);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception {
	Configuration conf = new Configuration();
        String[] remainingArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

	String[] tables = "date_dim time_dim item customer customer_demographics household_demographics customer_address store promotion warehouse ship_mode reason income_band call_center web_page catalog_page inventory store_sales store_returns web_sales web_returns web_site catalog_sales catalog_returns".split(" ");
	

        CommandLineParser parser = new BasicParser();
        conf.setInt("io.sort.mb", 4);
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption("s","scale", true, "scale");
        options.addOption("t","table", true, "table");
        options.addOption("d","dir", true, "dir");
        options.addOption("p", "parallel", true, "parallel");
        CommandLine line = parser.parse(options, remainingArgs);

        if(!(line.hasOption("scale") && line.hasOption("dir"))) {
          HelpFormatter f = new HelpFormatter();
          f.printHelp("GenTable", options);
          return 1;
        }
        
        int scale = Integer.parseInt(line.getOptionValue("scale"));
        String table = "all";
        if(line.hasOption("table")) {
          table = line.getOptionValue("table");
        }
        Path out = new Path(line.getOptionValue("dir"));

        int parallel = scale;

        if(line.hasOption("parallel")) {
          parallel = Integer.parseInt(line.getOptionValue("parallel"));
        }

        if(parallel == 1 || scale == 1) {
          System.err.println("The MR task does not work for scale=1 or parallel=1");
          return 1;
        }

	for (String t : tables)
	    {
		Path in = genInput(t, scale, parallel, conf);

		Path dsdgen = copyJar(new File("target/lib/dsdgen.jar"));
		URI dsuri = dsdgen.toUri();
		URI link = new URI(dsuri.getScheme(),
                    dsuri.getUserInfo(), dsuri.getHost(), 
                    dsuri.getPort(),dsuri.getPath(), 
                    dsuri.getQuery(),"dsdgen");

		conf.setInt("mapred.task.timeout",0);
		conf.setInt("mapreduce.task.timeout",0);
		DistributedCache.addCacheArchive(link, conf);
		Job job = new Job(conf, "GenTable+"+t+"_"+scale);
		job.setJarByClass(getClass());
		job.setNumReduceTasks(0);
		job.setMapperClass(DSDGen.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setInputFormatClass(NLineInputFormat.class);
		NLineInputFormat.setNumLinesPerSplit(job, 1);

		FileInputFormat.addInputPath(job, in);
		FileOutputFormat.setOutputPath(job, new Path(out.toString()+"/"+t));

        // use multiple output to only write the named files
        //LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
        //MultipleOutputs.addNamedOutput(job, "text", 
	//TextOutputFormat.class, LongWritable.class, Text.class);*//

        boolean success = job.waitForCompletion(true);

        // cleanup
        FileSystem fs = FileSystem.get(conf);
        
        fs.delete(in, false);
	    
        fs.delete(dsdgen, false);
	    }
        return 0;
    }

    public Path copyJar(File jar) throws Exception {
      MessageDigest md = MessageDigest.getInstance("MD5");
      InputStream is = new FileInputStream(jar);
      try {
        is = new DigestInputStream(is, md);
        // read stream to EOF as normal...
      }
      finally {
        is.close();
      }
      BigInteger md5 = new BigInteger(md.digest()); 
      String md5hex = md5.toString(16);
      Path dst = new Path(String.format("/tmp/%s.jar",md5hex));
      Path src = new Path(jar.toURI());
      Configuration conf = new Configuration();
      FileSystem fs = FileSystem.get(conf);
      fs.copyFromLocalFile(false, /*overwrite*/true, src, dst);
      return dst; 
    }

    public Path genInput(String table, int scale, int parallel, Configuration conf) throws Exception {
        long epoch = System.currentTimeMillis()/1000;
	
        Path in = new Path("/tmp/"+table+"_"+scale+"-"+epoch);
        FileSystem fs = FileSystem.get(new Configuration());
        FSDataOutputStream out = fs.create(in);
        for(int i = 1; i <= parallel; i++) {
          if(table.equals("all")) {
            out.writeBytes(String.format("./dsdgen -dir ./ -force Y -scale %d -parallel %d -child %d\n", scale, parallel, i));
          } else {
            out.writeBytes(String.format("./dsdgen -dir ./ -table %s -force Y -scale %d -parallel %d -child %d\n", table, scale, parallel, i));
          }
        }
        out.close();
        return in;
    }

    static String readToString(InputStream in) throws IOException {
      InputStreamReader is = new InputStreamReader(in);
      StringBuilder sb=new StringBuilder();
      BufferedReader br = new BufferedReader(is);
      String read = br.readLine();

      while(read != null) {
        sb.append(read);
        read =br.readLine();
      }
      return sb.toString();
    }

    static final class DSDGen extends Mapper<LongWritable,Text, NullWritable, Text> {
      private MultipleOutputs mos;
	  private Text outputText = new Text();
      protected void setup(Context context) throws IOException {
	  // mos = new MultipleOutputs(context);
	  //}      protected void cleanup(Context context) throws IOException, InterruptedException {
	  //mos.close();
	  
      }
      protected void map(LongWritable offset, Text command, Mapper.Context context) 
        throws IOException, InterruptedException {
        String parallel="1";
        String child="1";
       
        String[] cmd = command.toString().split(" ");

        for(int i=0; i<cmd.length; i++) {
          if(cmd[i].equals("$DIR")) {
	      File cmd_f = new File("tpc_tmp_data");
	      cmd_f.mkdirs();
            cmd[i] = cmd_f.getAbsolutePath();
          }
          if(cmd[i].equals("-parallel")) {
            parallel = cmd[i+1];
          }
          if(cmd[i].equals("-child")) {
            child = cmd[i+1];
          }
        }
	System.out.println(command.toString());
        Process p = Runtime.getRuntime().exec(cmd, null, new File("dsdgen/tools/"));
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));          
	String line;
	while ((line = br.readLine()) != null) {
            // process the line.
	    outputText.set(line);
	    context.write(NullWritable.get(), outputText);
	}
        int status = p.waitFor();	
	br.close();


        //if(status != 0) {
        //  String err = readToString(p.getErrorStream());
        //  throw new InterruptedException("Process failed with status code " + status + "\n" + err);
        //}

        //File cwd = new File("tpc_tmp_data");
        //final String suffix = String.format("_%s_%s.dat", child, parallel);

        //FilenameFilter tables = new FilenameFilter() {
        //  public boolean accept(File dir, String name) {
        //    return name.endsWith(suffix);
        //  }
        //};

        //for(File f: cwd.listFiles(tables)) {
        
        //  f.deleteOnExit();
      
      }
    }
}
