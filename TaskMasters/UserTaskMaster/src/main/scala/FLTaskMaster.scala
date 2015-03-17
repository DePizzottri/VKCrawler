import com.mongodb.casbah.Imports._

object Main extends App {
  val mongoClient = MongoClient("localhost", 27017)

  val db = mongoClient("VK_test_1")

  val users = db("friends_list")

  val curUserIDs = users.map { user => user("uid") }
  
  //> db.tasks.aggregate([{$match:{type:"friends_list"}},{$group:{_id:"$type", tot:{$push:{"data":"$data
//"}}}}])

  val tasks = db("tasks")
  
  tasks.ma

  for (gid <- curUserIDs.zipWithIndex.groupBy { id => id._2 / 100 }) {
    val bld = MongoDBList.newBuilder

    gid._2.foreach {
      iid =>
        {
          val id = iid._1
          bld += MongoDBObject("URL" -> s"https://api.vk.com/method/friends.get?user_id=$id")
        }
    }

    val task = MongoDBObject("type" -> "raw", "tag" -> "friends_list", "data" -> bld.result())

    tasks.insert(task)
  }

  mongoClient.close()
}