//package max.akkatest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import scala.concurrent.Future
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.OverflowStrategy
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import akka.util.ByteString
import spray.json._
import DefaultJsonProtocol._
import com.typesafe.config.ConfigFactory
import akka.stream.UniformFanInShape
import akka.stream.UniformFanOutShape
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTimeZone
import scala.util.Try
import akka.http.HostConnectionPoolSetup
import scala.concurrent.duration._
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.BidiShape
import akka.stream.FlowShape
import java.util.concurrent.ThreadLocalRandom
import io.scalac.amqp.Connection
import io.scalac.amqp.Message
import io.scalac.amqp.Exchange

case class WallPostsResponse(count: Int, items: JsArray)
case class WallPostsObject(response: WallPostsResponse)

object WallPostsResponseProtocol extends DefaultJsonProtocol {
  implicit val responseFormat = jsonFormat2(WallPostsResponse)
}

object WallPostsObjectProtocol extends DefaultJsonProtocol {
  import WallPostsResponseProtocol._
  implicit val wallPostsObjectFormat = jsonFormat1(WallPostsObject)
}

case class TaskData(id: Long, lastUseDate: Option[DateTime])

case class Task(`type`: String, data: Seq[TaskData])
case class TaskResult(`type`: String, data: JsValue)

object JodaDateTimeSupport extends DefaultJsonProtocol {
  implicit object JodaDateTimeFormat extends RootJsonFormat[DateTime] {
    override def write(d: DateTime) = JsString(d.toString(ISODateTimeFormat.dateTime().withZoneUTC()))

    override def read(v: JsValue) = v match {
      case JsString(s) => DateTime.parse(s, ISODateTimeFormat.dateTime().withZone(DateTimeZone.getDefault))
      case _           => deserializationError("ISO8601 date time expected")
    }
  }
}

object TaskDataJsonSupport extends DefaultJsonProtocol {
  import JodaDateTimeSupport._
  implicit val taskDataFormat = jsonFormat2(TaskData)
}

object TaskJsonSupport extends DefaultJsonProtocol {
  import TaskDataJsonSupport._
  implicit val taskFormat = jsonFormat2(Task)
}

object TaskResultJsonSupport extends DefaultJsonProtocol {
  implicit val taskResulFormat = jsonFormat2(TaskResult)
}

//object SecondsTick {
//  def apply(step: FiniteDuration) = {
//    val printSink = Sink.foreach { x: Int => println(x) }
//
//    val secondsGraph = FlowGraph.create(printSink) { implicit b =>
//      sink =>
//        import FlowGraph.Implicits._
//
//        val tickSource = Source(1.seconds, 1.seconds, 1)
//
//        val numberSource = Source.apply { () => Iterator.range(1, 20) }
//
//        val zip = b.add(Zip[Int, Int])
//
//        tickSource ~> zip.in0
//        numberSource ~> zip.in1
//
//        zip.out.map[Int] { case (a: Int, b: Int) => b } ~> sink.inlet
//    }
//
//    secondsGraph
//  }
//}

trait HttpConfig {
  val config = ConfigFactory.parseString("""
      akka {
        # log-config-on-start = on
        logLevel = "DEBUG"
      }
      akka.stream.materializer {
        subscription-timeout {
          mode = warn
          timeout = 30 seconds
        }
        debug-logging = on
        
        dispatcher = "akka.stream.materializer.crawler-dispatcher"
        
        crawler-dispatcher {
          type = "Dispatcher"
          executor = "thread-pool-executor"
          
          thread-pool-executor {
            # Keep alive time for threads
            keep-alive-time = 60s
    
            # Min number of threads to cap factor-based core number to
            core-pool-size-min = 16
    
            # The core pool size factor is used to determine thread pool core size
            # using the following formula: ceil(available processors * factor).
            # Resulting size is then bounded by the core-pool-size-min and
            # core-pool-size-max values.
            core-pool-size-factor = 5.0
    
            # Max number of threads to cap factor-based number to
            core-pool-size-max = 128
    
            # Minimum number of threads to cap factor-based max number to
            # (if using a bounded task queue)
            max-pool-size-min = 16
    
            # Max no of threads (if using a bounded task queue) is determined by
            # calculating: ceil(available processors * factor)
            max-pool-size-factor  = 5.0
    
            # Max number of threads to cap factor-based max number to
            # (if using a  bounded task queue)
            max-pool-size-max = 128
    
            # Specifies the bounded capacity of the task queue (< 1 == unbounded)
            task-queue-size = -1
    
            # Specifies which type of task queue will be used, can be "array" or
            # "linked" (default)
            task-queue-type = "linked"
    
            # Allow core threads to time out
            allow-core-timeout = on
          }          
        }                
      }
      akka.http.host-connection-pool {
          max-connections = 32
          max-open-requests = 1024
          pipelining-limit = 1
        }
      amqp {
        addresses = [
          { host = "192.168.1.9", port = 5672 }
        ],
        virtual-host = "/",
        username = "vkcrawler",
        password = "vkcrawler",
        heartbeat = disable,
        timeout = infinite,
        automatic-recovery = false,
        recovery-interval = 5s
        ssl = disable        
      }
      """)
}

object Crawler extends HttpConfig {
  type VKID = Long

  implicit val system = ActorSystem("TaskClientSystem", config.withFallback(ConfigFactory.load()))
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val crawlerConnections =
    Http().cachedHostConnectionPool[Int]("192.168.1.9", 8080)

  //http://api.vk.com/method/wall.get?owner_id=30542028&offset=120&count=1&v=5.37
  val VKConnectionPool =
    Http().cachedHostConnectionPool[VKID]("api.vk.com")
  //Http().cachedHostConnectionPool[Long]("example.com")
  //Http().outgoingConnection("api.vk.com")
  //Http().superPool[Int]()
    
//  val flattenInner:Flow[(Source[ByteString, Any], VKID), (ByteString, VKID), Unit] = Flow.fromGraph(
//    FlowGraph.create() { implicit b =>
//      import FlowGraph.Implicits._
//  
//      val unzip = b.add(Unzip[Source[ByteString, Any], VKID])
//  
//      val flatten = Flow[Source[ByteString, Any]]
////        .fold(Source.empty[ByteString]) { case (prevElem, newElem) =>
////          prevElem.merge(newElem)
////        }
////        .flatMapConcat(identity)
////        .buffer(100000, OverflowStrategy.fail)
//  
//      val zip = b.add(Zip[ByteString, VKID])
//  
//      unzip.out0 ~> flatten ~> zip.in0
//      unzip.out1 ~> zip.in1
//  
//      FlowShape(unzip.in, zip.out)
//  })
    
//  val mergeID = Flow.fromGraph[(Source[ByteString, Any], VKID), Source[(ByteString, VKID), Any], Unit] (
//    FlowGraph.create() { implicit b =>
//      import FlowGraph.Implicits._
//      val unzip = b.add(Unzip[Source[ByteString, Any], VKID])
//      val zip = b.add(Zip[ByteString, VKID])
//      val toSourceMapper = Flow[VKID].map { x => Source.single(x) }
//      
////      unzip.out0 ~> zip.in0
////      unzip.out1 ~> toSourceMapper ~> zip.in1
//      
//      FlowShape(unzip.in, zip.out)
//    })
  
  val convertResponsePair = //: Flow[(Try[HttpResponse], VKID), (ByteString, VKID), Unit] =  
    Flow[(Try[HttpResponse], Long)]
      .map { x =>
        x._1 match {
          case Success(resp) if resp.status == StatusCodes.OK => (resp.entity.dataBytes, x._2)
          case Success(resp) => println("Wrong status: " + resp.status); (Source.empty[ByteString], x._2)
          case Failure(f)    => println("FAIL " + f.toString()); (Source.empty[ByteString], x._2)
        }
      }
      .mapAsyncUnordered(4) { x =>
        Future {
          (foldByteString(x._1), x._2)
        }
      }
      //.via(flattenInner)

  val fakeVKAnswerFlow: Source[JsArray, Unit] =
    Source(() => Iterator.continually(JsArray(Seq("""{hello:"fakeVKAnsPayload"}""".toJson): _*)))

  def foldByteString(x: Source[ByteString, Any]) = {
    val folded = x
    .fold(ByteString.empty) { (a, b) =>
      a ++ b
    }
    folded        
  }
    
  val idsSource: Source[VKID, Http.HostConnectionPool] =
    Source { () => Iterator.continually(HttpRequest(uri = s"/getTask?version=1&types=wall_posts") -> 42) }
      .viaMat(crawlerConnections)(Keep.right)
      .map { x =>
        x._1 match {
          case Success(resp) => resp.entity.dataBytes
          case Failure(f)    => println("Failure in getting id from tasks source"); Source.empty[ByteString]
        }
      }
      .mapAsyncUnordered(4) { x =>
        Future {
          foldByteString(x)
        }
      }
      .flatMapConcat(identity)
      .mapAsyncUnordered(4) { x =>
        Future {
          import TaskJsonSupport._
          x.utf8String.parseJson.convertTo[Task].data.map(x => x.id).toList
        }
      }
      .mapConcat(identity)

  private val ids = Array(1l, 5l, 7l, 8l)
  val fakeIdsSource: Source[VKID, Unit] =
    Source { () => Iterator.continually(ThreadLocalRandom.current().nextInt(100000)) }

  def VKAnswerFlow = Flow.fromGraph[VKID, (Source[ByteString, Any], VKID), Unit] (
    FlowGraph.create() { implicit b =>
      import FlowGraph.Implicits._
      val src = b.add(Flow[VKID])
  
      val requests = src.outlet
        .map { x => HttpRequest(uri = s"/method/wall.get?owner_id=$x&count=1&v=5.37") -> x }
  
      val refinedRequests = requests ~> VKConnectionPool ~> convertResponsePair 
      
      FlowShape(src.inlet, refinedRequests.outlet)
    }
  )

//  def getCountFlow: Flow[(ByteString, VKID), (Int, VKID), Unit] =
//    Flow[(ByteString, VKID)]
//      .mapAsyncUnordered(4) { x =>
//        Future {
//          Try {
//            import WallPostsObjectProtocol._
//            (x._1.utf8String.parseJson.convertTo[WallPostsObject].response.count, x._2)
//          }
//        }
//      }
//      .filter {
//        case Success((v, _)) => true
//        case Failure(_)      => false
//      }
//      .map { x =>
//        x.get
//      }
//      .map { x =>
//        println(s"Id ${x._2} have ${x._1} wall posts")
//        x
//      }
      
  def getCountFlow2: Flow[ByteString, Int, Unit] =
    Flow[ByteString]
      .mapAsyncUnordered(4) { x =>
        Future {
          Try {
            import WallPostsObjectProtocol._
            x.utf8String.parseJson.convertTo[WallPostsObject].response.count
          }
        }
      }
      .filter {
        case Success(_) => true
        case Failure(f) => false
      }
      .map { x =>
        x.get
      }
      

  def allWallPostsGetFlow = Flow.fromGraph[(Int, VKID), Source[(ByteString, Long), Unit], Unit]( FlowGraph.create() { implicit b =>
    import FlowGraph.Implicits._
    val src = b.add(Broadcast[(Int, Long)](1))
    
    val window = 100
    
    val wallPostsRequestsFlow = src
      .map {
        case (count, id) =>
          val offsets = (0 to count / window).map { x => x * window }
          val windows = offsets.map((_, window))
          Source(windows).map {
            case (offset, count) =>
              HttpRequest(uri = s"/method/wall.get?owner_id=$id&offset=$offset&count=$window&v=5.37") -> (id, offset, count)
          }
      }
      .mapAsyncUnordered(8) { x =>
        Future {
          x
          .map { a =>
            //println(s"Requesting id ${a._2._1} offset=${a._2._2} count=${a._2._3}")
            (a._1, a._2._1)
          }
          .via(VKConnectionPool)
          .via(convertResponsePair)
        }
      }
      .mapAsyncUnordered(4) { x=>
        Future{
          x
          .mapAsyncUnordered(4) { y=>
            y._1.runFold(ByteString.empty){(a,b) => a++b}.map(bs => (bs, y._2))
          }
        }
      }
      //.flatten(FlattenStrategy.concat)

    val resultStream = wallPostsRequestsFlow// ~> VKConnectionPool ~> convertResponsePair
    //val resultStream = wallPostsRequestsFlow ~> convertResponsePair
    FlowShape(src.in, resultStream.outlet)
      
//    val resultStream = wallPostsRequestsFlow ~> connectSingle("api.vk.com") ~> convertResponsePair
//    (src.in, resultStream.outlet)
  })

  def byteStringToResponse: Flow[ByteString, WallPostsResponse, Unit] = //Flow() { implicit b =>
    Flow[ByteString]
      .mapAsyncUnordered(4) { x =>
        Future {
          Try {
            import WallPostsObjectProtocol._
            (x.utf8String.parseJson.convertTo[WallPostsObject].response)
          }
        }
      }
      .filter {
        case Success(v) => true
        case Failure(_) => false
      }
      .map { x =>
        x.get
      }

  val webIfaceConnection =
    Http().outgoingConnection("localhost", 8080)

  def postToWEB: Flow[WallPostsResponse, HttpRequest, Unit] =
    Flow[WallPostsResponse]
      .map { wallPost =>
        wallPost.items
      }
      .conflate[Seq[JsValue]]((arr) => arr.convertTo[Seq[JsValue]]) {
        case (aggregeted, newElem) =>
          aggregeted ++ newElem.convertTo[Seq[JsValue]]
      }
      //      .map { wallPost =>
      //        wallPost.convertTo[List[JsValue]]
      //      }
      //      .mapAsyncUnordered(8) { wallPost => Future {
      //          val v = wallPost.items.convertTo[List[JsValue]]
      //          val ret = (1 to 50).foldLeft(List.empty[JsValue]){(a,b) => v ::: a}
      //          JsArray(ret: _*)
      //        }
      //      }
      .map { wallPosts =>
        import WallPostsObjectProtocol._
        import TaskResultJsonSupport._
        import DefaultJsonProtocol._

        //println(wallPosts.length)

        val data = ByteString(TaskResult("wall_posts", JsArray(wallPosts: _*)).toJson.compactPrint)
        HttpRequest(
          method = HttpMethods.POST,
          uri = "/postTask",
          entity = HttpEntity(ContentType(MediaTypes.`application/json`), data))
      }
}

object HttpClient extends HttpConfig {
  def run {
    import Crawler._

    //    val connection = Connection(this.config)
    //    //val queue = connection.queueBind("vktest", "vktest", "vktest"
    //    val exchange = connection.publish(exchange = "vktest", routingKey = "vktest")

    import scala.concurrent.ExecutionContext.Implicits.global

    var cnt = 0
    
    val responseFuture =
      fakeIdsSource
        .via(VKAnswerFlow)
        .mapAsyncUnordered(4) { flow =>
          flow._1.via(getCountFlow2).runFold(-1)((a,b) => b).map{x => (x, flow._2)}
        }
        .filter{ _._1 != -1}
//        .map{ x =>
//          //println(s"Id ${x._2} have ${x._1} wall posts")
//          x
//        }
        .buffer(10000, OverflowStrategy.backpressure)
        .via(allWallPostsGetFlow)
//        .map { x=>
//          
//        }
//        .mapAsyncUnordered(4) { src =>
//          val f= src
          .mapAsyncUnordered(4) { src => 
            src
            .map{case (bs, id) => bs}
            .via(byteStringToResponse)
            .via(postToWEB)
            .via(webIfaceConnection)
            //.runForeach { x => println("Id fetched") }
            .runWith(Sink.ignore)
          }
//          .runWith(Sink.ignore)
//          f.onComplete { 
//            case Success(v) => cnt+=1; println(s"Id fetched $cnt") 
//            case _ => println("Id Failed")
//            }
//          f
//        }
//        .buffer(10000, OverflowStrategy.backpressure)
//        .via(byteStringToResponse)
//        .via(postToWEB)
//        .via(webIfaceConnection)
        .runWith(Sink.ignore)
//        .runForeach { x =>
//          //        println(x._1.get.status)
////          println(x._2)
//        }
//        .runWith(Sink.head)
        
    //          .map { x =>
    //            Message(ByteString(x))
    //          }
    //          .to(Sink(exchange)).run()
    //    .runForeach { x => println(x.utf8String) }

    //    val responseFuture = postsAnsSource
    //      .runForeach { x =>
    ////        println(x)
    ////        println(x.status)
    //      }
    //
    //        responseFuture onComplete { x =>
    //          println("End")
    //          println(x)
    //          Http().shutdownAllConnectionPools() onComplete { x =>
    //            println("End")
    //            system.shutdown()
    //          }
    //        }
  }
}

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._

object HttpServer extends HttpConfig {
  def run {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import scala.concurrent.ExecutionContext.Implicits.global
    val connection = Connection(config)
    //val queue = connection.queueBind("vktest", "vktest", "vktest")

    val serverSource = Http().bind(interface = "localhost", port = 8080)

    val requestHandler: HttpRequest => Future[HttpResponse] = {
      //      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      //        HttpResponse(entity = HttpEntity(MediaTypes.`text/html`,
      //          "<html><body>Akka HTTP test</body></html>"))
      //
      //      case HttpRequest(GET, Uri.Path("/getTask"), _, entity, _) => {
      //        entity
      //          .dataBytes
      //          .runFold(ByteString.empty)((a, b) => a ++ b)
      //          .onComplete { data =>
      //            system.log.info("getTask: " + data.map { _.utf8String }.getOrElse("Error"))
      //          }
      //        HttpResponse(entity = "Stub")
      //      }

      case HttpRequest(POST, Uri.Path("/postTask"), _, entity, _) => Future {
        val exchange = connection.publish(exchange = "vktest", routingKey = "vktest")
        val src = entity
          .dataBytes
          .fold(ByteString.empty)((a, b) => a ++ b)
          .map { x =>
            
            Message(x)
          }
          .to(Sink(exchange))
          .run()
        //        .onComplete { data => 
        //          system.log.info("getTask: " + data.map{_.utf8String}.getOrElse("Error"))
        //        }
        HttpResponse(entity = "Ok")
      }

      case _: HttpRequest =>
        Future { HttpResponse(404, entity = "Unknown resource!") }
    }

    val bindingFuture: Future[Http.ServerBinding] =
      serverSource.to(Sink.foreach { connection =>
        system.log.info("Accepted new connection from " + connection.remoteAddress)

        connection handleWithAsyncHandler requestHandler
      }).run()
  }
}

object AkkaHttpTest extends App {
  if (args.contains("server"))
    HttpServer.run
  if (args.contains("client"))
    HttpClient.run
}
