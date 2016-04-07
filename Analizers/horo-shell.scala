    import org.apache.hadoop.conf.Configuration
    import org.bson.BSONObject
    import com.mongodb.hadoop.{
    MongoInputFormat, MongoOutputFormat,
    BSONFileInputFormat, BSONFileOutputFormat}
    import com.mongodb.hadoop.io.MongoUpdateWritable
    import org.apache.spark._
    import org.apache.spark.graphx._
    import org.apache.spark.rdd.RDD
    
    val mongoConfig = new Configuration()

    mongoConfig.set("mongo.input.uri",
      "mongodb://localhost:27017/vk_ulan_ude.friends")

  //    val sparkConf = new SparkConf()
  //    val sc = new SparkContext("spark://meows1:7077", "SparkExample", sparkConf)

    // Create an RDD backed by the MongoDB collection.
    val documents = sc.newAPIHadoopRDD(
      mongoConfig,                // Configuration
      classOf[MongoInputFormat],  // InputFormat
      classOf[Object],            // Key type
      classOf[BSONObject])        // Value type

    val idfr = documents.map(x=> (Long.unbox(x._2.get("id")),x._2.get("friends").asInstanceOf[com.mongodb.BasicDBList].toArray.map(a=>Long.unbox(a)))).flatMap(x => x._2.map(a=> (x._1,a)))

    val mongoConfig2 = new Configuration()

    mongoConfig2.set("mongo.input.uri",
      "mongodb://localhost:27017/vk_ulan_ude.user_info")

    val documents2 = sc.newAPIHadoopRDD(mongoConfig2,classOf[MongoInputFormat],classOf[Object],classOf[BSONObject])

    val idbr_0 = documents2.map(x => (Int.unbox(x._1).toLong,{
      val some = x._2.get("birthday")
      if (some==null) (0,0)
      else {
        val thing = some.asInstanceOf[BSONObject].get("day")
        val hmm = some.asInstanceOf[BSONObject].get("month")
        (if (thing==null) 0
        else Int.unbox(thing),
          if (hmm==null) 0
          else Int.unbox(hmm))

      }
    }))

    val idbr = idbr_0.map(x => (x._1,{
      def mdzod(a:Int, b:Int) = a match {
        case 0 => 0
        case 1 => if (b >= 20) 1 else 12
        case 2 => if (b >= 19) 2 else 1
        case 3 => if (b >= 21) 3 else 2
        case 4 => if (b >= 20) 4 else 3
        case 5 => if (b >= 21) 5 else 4
        case 6 => if (b >= 21) 6 else 5
        case 7 => if (b >= 23) 7 else 6
        case 8 => if (b >= 23) 8 else 7
        case 9 => if (b >= 23) 9 else 8
        case 10 => if (b >= 23) 10 else 9
        case 11 => if (b >= 22) 11 else 10
        case 12 => if (b >= 22) 12 else 11
      }
      mdzod(x._2._2,x._2._1)
    }))

    val graphZ = Graph(VertexRDD(idbr),idfr.map(x=>Edge(x._1,x._2,"friends")))

    val zodiacs = Map (
      0 -> "Хаос",
      1 -> "Водолей",
      2 -> "Рыбы",
      3 -> "Овен",
      4 -> "Телец",
      5 -> "Близнецы",
      6 -> "Рак",
      7 -> "Лев",
      8 -> "Дева",
      9 -> "Весы",
      10 -> "Скорпион",
      11 -> "Стрелец",
      12 -> "Козерог"
    )

    val forzodcntbr = (0 to 12).map(x=>(x,graphZ.vertices.filter(a=>a._2==x).count)).toMap

//    val keys_wo1000 = graphZ.collectEdges(EdgeDirection.Out).filter(x => x._2.size <= 1000).keys
//
//    val br_keys = sc.broadcast(keys_wo1000.collect())
//
//    val zzave = graphZ.triplets.filter(x=>br_keys.value.contains(x.srcAttr)).map(x=>((x.srcAttr,x.dstAttr),1)).reduceByKey(_+_).collect()

   def horWithEdgesUnder(ppl: Int) = {
     val zer = graphZ.triplets.map(x => ((x.srcId, x.srcAttr), x.dstAttr)).aggregateByKey(Vector.empty[Int])(_ :+ _, _ ++ _).filter(_._2.size <= ppl).cache()

     val fir = zer.map(x => (x._1._2, x._2)).aggregateByKey(Vector.empty[Int])(_ ++ _, _ ++ _).
       map(x => (x._1, x._2.map(a => (a, 1)).groupBy(a => a._1).map(a => (a._1, a._2.map(v => v._2).sum)))).collectAsMap().toSeq.map(a=>(a._1,a._2.toSeq.sortBy(_._1))).sortBy(_._1).
       map(_._2.map(_._2).mkString(",")).mkString("\n")

     val sec = zer.keys.map(a=>(a._2,1)).reduceByKey((a,b) => a+b).collectAsMap().toSeq.sortBy(_._1).
       map(_._2).mkString(",")

     import java.io._

     val writer = new PrintWriter(new File(s"output$ppl.csv"))

     writer.write(fir+"\n"+sec)
     writer.close()

   }

