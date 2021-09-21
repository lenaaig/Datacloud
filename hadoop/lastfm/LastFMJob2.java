package datacloud.hadoop.lastfm;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;


public class LastFMJob2 {
	public static class LFMJ2Mapper extends Mapper<LongWritable, Text, IntWritable, Text>{

		public String extract_something(String line, int n) {
			String[] split = line.split(" ");
			System.err.println("split"+split.length);
			return split[n]; // 1 : trackid | 2 : local | 3 : radio
		}


		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			String trackid =extract_something(line, 1);
			String local =extract_something(line, 2);
			String radio =extract_something(line, 3);
			String skip =extract_something(line, 4);


			IntWritable t = new IntWritable();

			Text lr=new Text();
			lr.set(Integer.valueOf(local)+Integer.valueOf(radio)+" "+skip);
			t.set(Integer.valueOf(trackid));
			context.write(t, lr);
			
		}
	}

	public static class LFMJ2Reducer extends Reducer<IntWritable,Text,IntWritable,Text> {

		public void reduce(IntWritable key, Iterable<Text> value, Context context) throws IOException, InterruptedException {
			
			int lr =0, s=0;
			for (Text c : value) {
				String[] tab = c.toString().split(" ");
				lr+=Integer.parseInt(tab[0]);
				s +=Integer.parseInt(tab[1]);
			}
			Text t = new Text();
			t.set(" " +lr+" "+ s);
			context.write(key, t);
		}
	}


	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.setBoolean("mapreduce.map.speculative", true);
		conf.setBoolean("mapreduce.reduce.speculative", true);
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: mapper <in> <out>");
			System.exit(2);
		}
		Job job = Job.getInstance(conf, "mapper");
		job.setJarByClass(LastFMJob2.class);//permet d'indiquer le jar qui contient l'ensemble des .class du job à partir d'un nom de classe
		job.setMapperClass(LFMJ2Mapper.class); // indique la classe du Mapper
		job.setReducerClass(LFMJ2Reducer.class); // indique la classe du Reducer
		job.setMapOutputKeyClass(IntWritable.class);// indique la classe  de la clé sortie map
		job.setMapOutputValueClass(Text.class);// indique la classe  de la valeur sortie map    
		job.setOutputKeyClass(IntWritable.class);// indique la classe  de la clé de sortie reduce    
		job.setOutputValueClass(Text.class);// indique la classe  de la clé de sortie reduce
		job.setInputFormatClass(TextInputFormat.class); // indique la classe  du format des données d'entrée
		job.setOutputFormatClass(TextOutputFormat.class); // indique la classe  du format des données de sortie
		//job.setPartitionerClass(NoodlePartitioner.class);// indique la classe du partitionneur
		job.setNumReduceTasks(1);// nombre de tâche de reduce : il est bien sur possible de changer cette valeur (1 par défaut)


		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));//indique le ou les chemins HDFS d'entrée
		final Path outDir = new Path(otherArgs[1]);//indique le chemin du dossier de sortie
		FileOutputFormat.setOutputPath(job, outDir);
		final FileSystem fs = FileSystem.get(conf);//récupération d'une référence sur le système de fichier HDFS
		if (fs.exists(outDir)) { // test si le dossier de sortie existe
			fs.delete(outDir, true); // on efface le dossier existant, sinon le job ne se lance pas
		}

		System.exit(job.waitForCompletion(true) ? 0 : 1);// soumission de l'application à Yarn
	}


}


