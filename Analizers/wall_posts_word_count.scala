//http://mvnrepository.com/artifact/org.mongodb.mongo-hadoop/mongo-hadoop-core/1.4.0
//http://mvnrepository.com/artifact/org.mongodb/mongo-java-driver/3.0.2
//set ADD_JARS=D:\Code\Spark\spark-1.4.0\bin\mongo-hadoop-core-1.4.0.jar,D:\Code\Spark\spark-1.4.0\bin\mongo-java-driver-3.0.2.jar 
//https://github.com/srccodes/hadoop-common-2.2.0-bin/archive/master.zip
//set HADOOP_HOME="D:/Code/Spark/spark-1.4.0/hadoop-common-2.2.0-bin-master" 

import org.apache.hadoop.conf.Configuration
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd.RDD

import org.bson._
import com.mongodb.hadoop.{
  MongoInputFormat, MongoOutputFormat,
  BSONFileInputFormat, BSONFileOutputFormat}

def getNewFile(path:String) = {
  val f = new java.io.File(path)
  new java.io.PrintWriter(f)
}

val mongoConfig = new Configuration()
mongoConfig.set(
  "mongo.input.uri",
  "mongodb://192.168.1.9:27017/vkcrawler_test_1.wall_posts")

val documents = sc.newAPIHadoopRDD(
  mongoConfig,
  classOf[MongoInputFormat],
  classOf[Object],
  classOf[BSONObject])

val wallTexts = documents.filter{ obj => 
  obj._2.containsField("text") && obj._2.get("text") != ""
}

val postText = wallTexts.map{ obj =>
  obj._2.get("text").asInstanceOf[String]
}

import scala.collection.immutable.StringOps

val words = postText.
  flatMap{ post =>
    val smartWord = new StringOps(post)
    smartWord.split(Array(' ', ',', '.', '!', '?', '*', '(', ')', '/', ':'))
  }.
  filter{ _ != "" }.
  map{_.toLowerCase}

val freqMap = words.map{(_,1)}.groupByKey.mapValues{ x => x.size }

val top500 = freqMap.takeOrdered(500)(Ordering.by[(String, Int), Int](-_._2))