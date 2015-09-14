package com.vkcrawler.WEBInterface

import akka.actor.Actor
import spray.routing.Directives._
import spray.routing.ValidationRejection
import spray.http.StatusCodes
import spray.json._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.ConnectionFactory
import com.vkcrawler.DataModel._
import com.vkcrawler.DataModel.SprayJsonSupport._
import com.vkcrawler.DataModel.SprayJsonSupport.FriendsListTaskResultJsonSupport._
import com.vkcrawler.WEBInterface.MongoDB._
import spray.routing.HttpServiceActor
import spray.routing.HttpService
import com.rabbitmq.client.AMQP.Channel
import akka.util.Timeout

trait TaskResultProcessorComponent {

  val processor: TaskResultProcessor

  trait TaskResultProcessor {
    def process(task: FriendsListTaskResult): Unit
  }

}

trait RabbitTaskResultProcessorComponent extends TaskResultProcessorComponent {

  override val processor = new RabbitTaskResultProcessor()

  class RabbitTaskResultProcessor extends TaskResultProcessor {
    val conf = ConfigFactory.load()

    //connect to RabbitMQ
    val EXCHANGE_NAME = "VKCrawler"
    val ROUTING_KEY = "info_and_graph"

    val factory = new ConnectionFactory()
    factory.setHost(conf.getString("RabbitMQ.host"))
    factory.setUsername(conf.getString("RabbitMQ.username"))
    factory.setPassword(conf.getString("RabbitMQ.password"))
    val connection = factory.newConnection()
    val channel = connection.createChannel()

    channel.exchangeDeclare(EXCHANGE_NAME, "direct")

    override def process(task: FriendsListTaskResult) {
      channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, MessageProperties.PERSISTENT_TEXT_PLAIN, task.toJson.compactPrint.getBytes());
    }
  }
}

trait TaskGetterComponent {
  val getter: TaskGetter 

  trait TaskGetter {
    def getTask(types: Array[String]):Either[Task, JsObject]
  }
}

trait MongoDBTaskGetterComponent extends TaskGetterComponent {
  override val getter = new MongoDBTaskGetter()   
  class MongoDBTaskGetter extends TaskGetter {
    override def getTask(types: Array[String]):Either[Task, JsObject] = {
      MongoDBSource.getTask(types)
    }
  }
}

trait ConnectionHandler extends HttpService 
{
  this: TaskResultProcessorComponent with TaskGetterComponent =>
  
  lazy val route = root ~ hello ~ getTask ~ postTask //~ stop

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
          types match {
            case "" => reject(ValidationRejection("""Parameter "types" can not be empty"""))
            case _ => detach() {
              complete {
                getter.getTask(types.split(","))
              }
            }
          }
        }
      }
    }

  lazy val postTask = {
    path("postTask") {
      post {
        entity(as[FriendsListTaskResult]) { res =>
          processor.process(res)
          complete("Ok")
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

//  lazy val stop = {
//    path("shutdown") {
//      get {
//        complete {
//          //graceful stop
//          implicit val timeout = Timeout(15 seconds)
//          implicit val executionContext = system.dispatcher
//          Await.result(IO(Http) ? Http.CloseAll, 15 seconds)
//          system.stop(IO(Http))
//          system.shutdown();
//          "Ok"
//        }
//      }
//    }
//  }  
}

class ConnectionHandlerActor extends Actor
  with RabbitTaskResultProcessorComponent
  with MongoDBTaskGetterComponent  
  with ConnectionHandler 
{
  def actorRefFactory = context
  override def receive = runRoute(route)
}