import spray.routing.SimpleRoutingApp
import akka.actor.ActorSystem
import spray.http.StatusCodes
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBList
import scala.io.Codec
import com.mongodb.util.JSON

import spray.json._

case class Data(URL: String)
case class Task(`type`: String, tag: String, data: List[String])

case class Friends(list: List[Long])

object TaskJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val dataFormat = jsonFormat1(Data)
  implicit val taskFormat = jsonFormat3(Task)
}

object FriendsJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val friendsFormat = jsonFormat1(Friends)
}

object TaskDB {
  def apply(o:DBObject) = new Task(
        o("type").asInstanceOf[String], 
        o("tag").asInstanceOf[String], 
        o("data").asInstanceOf[BasicDBList].map((b:Any) => {
          b.asInstanceOf[DBObject].as[String]("URL")
          }
        ).toList
      )
}


object Application extends App with SimpleRoutingApp {
  val mongoClient = MongoClient("localhost", 27017)
  val db = mongoClient("VK_test")

  def getTaskFromDB(`type`:String):Task = {
    val tasks = db("tasks")
    val optTask = tasks.findAndModify(MongoDBObject("type" -> `type`), MongoDBObject("usecount" -> 1), $inc("usecount" -> 1))
     
    val task = optTask.get
    
    TaskDB(task)
    
    //com.mongodb.util.JSON.serialize(task)
    
    //spray.json.JsonParser.Json.toJson(st)
    
//    new Task(
//        task("type").asInstanceOf[String], 
//        task("tag").asInstanceOf[String], 
//        task("data").asInstanceOf[BasicDBList].asInstanceOf[List[String]]        
//    )
  }

  implicit val system = ActorSystem("my-system")

  lazy val root =
    path("") {
      get {
        redirect("hello", StatusCodes.TemporaryRedirect)
      }
    }
  
  import TaskJsonSupport._

  lazy val getTask =
    path("getTask") {
      get {
        parameters("version".as[Int], "types".as[String]) { (version, types) =>
            complete{ 
              getTaskFromDB(types)        
            }
        }
      }
    }

  lazy val hello =
    path("hello") {
      get {
        complete {
          <h1>Welcome to VC Crawler Task frontend</h1>
        }
      }
    }

  startServer(interface = "localhost", port = 8080) {
    root ~ hello ~ getTask
  }
}