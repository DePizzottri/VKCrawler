import org.apache.hadoop.conf.Configuration
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd.RDD

import org.bson._
import com.mongodb.hadoop.{
  MongoInputFormat, MongoOutputFormat,
  BSONFileInputFormat, BSONFileOutputFormat}

val mongoConfig = new Configuration()
mongoConfig.set(
  "mongo.input.uri",
  "mongodb://192.168.1.9:27017/vkcrawler_test_1.infongraph")

val documents = sc.newAPIHadoopRDD(
  mongoConfig,
  classOf[MongoInputFormat],
  classOf[Object],
  classOf[BSONObject])

val withYearDataRdd = documents.filter{ x =>
  x._2.containsField("birthday") &&
  x._2.get("birthday") != null &&
  x._2.get("birthday").asInstanceOf[BSONObject].containsField("year") &&
  x._2.get("birthday").asInstanceOf[BSONObject].get("year") != null
}

//(id, [fid, city], year)
val convertedDataRdd = withYearDataRdd.map{ x =>
  (x._2.get("uid").asInstanceOf[Long],
  x._2.get("friends").asInstanceOf[BasicDBList].toArray.map{y => (y.asInstanceOf[BSONObject].get("uid").asInstanceOf[Long], y.asInstanceOf[BSONObject].get("city").asInstanceOf[Int])},
  x._2.get("birthday").asInstanceOf[BSONObject].get("year").asInstanceOf[Int])
}

//(id, [fid], year)
val friendsCityFilteredRdd = convertedDataRdd.map{ x =>
  (x._1,
  x._2.filter{y => y._2 == 57}.map{y => y._1},
  x._3
  )
}.filter {x =>
  x._3 < 2000 && x._3 > 1989
}

friendsCityFilteredRdd.cache()

//(id, fid, year)
val userFriendPairRdd = friendsCityFilteredRdd.flatMap{ x =>
  x._2.map{y => (x._1, y, x._3)}
}

//(fid, (id, year))
val friendUserPairRdd = userFriendPairRdd.map{ x=>
  (x._2, (x._1, x._3))
}

//(id, year)
val userYearRdd = friendsCityFilteredRdd.map{ x=>
  (x._1, x._3)
}

//(fid, ((id, year), year))
val friendsWithYearsRdd = friendUserPairRdd.join(userYearRdd)

//((id, year), (fid, year))
val friendsWithYearsCorrectedRdd = friendsWithYearsRdd.map{ x=>
  (x._2._1, (x._1, x._2._2))
}

//((id, year), [(fid, year)])
val friendsWithYearGroupedRdd = friendsWithYearsCorrectedRdd.groupByKey()

friendsWithYearGroupedRdd.cache()

//(id, [fid])
val friendsWithSameYearRdd = friendsWithYearGroupedRdd.map{ x =>
  (x._1._1,
  x._2.filter{y => math.abs(y._2 - x._1._2) < 2}.map{y => y._1}
  )
}

//(if, [fid], count)
val superFriendsRdd = friendsWithSameYearRdd.map{ x =>
  (x._1, x._2, x._2.size)
}

val result = superFriendsRdd.takeOrdered(100)(Ordering.by[(Long, Iterable[Long], Int), Int](-_._3))

import java.io._

val writer = new PrintWriter(new File("result100.csv" ))

result.foreach { x =>
  writer.write(x._1.toString)
  writer.write(";")

  writer.write(x._2.toString)
  writer.write(";")

  writer.write(x._3.toString)
  writer.write(";")
  writer.write("\n")
}

writer.flush()
writer.close()
