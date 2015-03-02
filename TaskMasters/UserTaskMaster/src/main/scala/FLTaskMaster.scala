import com.mongodb.casbah.Imports._

object Main extends App {
  val mongoClient = MongoClient("localhost", 27017)

  val db = mongoClient("VK_test")

  val users = db("users")

  val userIDs = users.map { user => user("uid") }

  val tasks = db("tasks")

  for (gid <- userIDs.zipWithIndex.groupBy { id => id._2 / 100 }) {
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