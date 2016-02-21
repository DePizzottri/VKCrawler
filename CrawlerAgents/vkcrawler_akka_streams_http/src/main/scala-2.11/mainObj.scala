import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream._
import akka.stream.scaladsl.Source
import scala.concurrent.Future
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import scala.util.Failure
import scala.util.Success
import akka.util.ByteString
import spray.json._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import scala.util.Try
import java.util.concurrent.ThreadLocalRandom
import _root_.io.scalac.amqp.Connection
import _root_.io.scalac.amqp.Message




trait HttpConfig {
  val config = ConfigFactory.parseString("""
    akka {
      # log-config-on-start = on
      logLevel = "DEBUG"
       stream {

         # Default flow materializer settings
         materializer {

           # Initial size of buffers used in stream elements
           initial-input-buffer-size = 4
           # Maximum size of buffers used in stream elements
           max-input-buffer-size = 16

           # Fully qualified config path which holds the dispatcher configuration
           # to be used by FlowMaterialiser when creating Actors.
           # When this value is left empty, the default-dispatcher will be used.
           dispatcher = ""

           # Cleanup leaked publishers and subscribers when they are not used within a given
           # deadline
           subscription-timeout {
             # when the subscription timeout is reached one of the following strategies on
             # the "stale" publisher:
             # cancel - cancel it (via `onError` or subscribing to the publisher and
             #          `cancel()`ing the subscription right away
             # warn   - log a warning statement about the stale element (then drop the
             #          reference to it)
             # noop   - do nothing (not recommended)
             mode = warn

             # time after which a subscriber / publisher is considered stale and eligible
             # for cancelation (see `akka.stream.subscription-timeout.mode`)
             timeout = 30s
           }

           # Enable additional troubleshooting logging at DEBUG log level
           debug-logging = on

           # Maximum number of elements emitted in batch if downstream signals large demand
           output-burst-limit = 1000

           # Enable automatic fusing of all graphs that are run. For short-lived streams
           # this may cause an initial runtime overhead, but most of the time fusing is
           # desirable since it reduces the number of Actors that are created.
           auto-fusing = on


         }

         # Fully qualified config path which holds the dispatcher configuration
         # to be used by FlowMaterialiser when creating Actors for IO operations,
         # such as FileSource, FileSink and others.
         blocking-io-dispatcher = "akka.stream.default-blocking-io-dispatcher"

         default-blocking-io-dispatcher {
           type = "Dispatcher"
           executor = "thread-pool-executor"
           throughput = 1

           thread-pool-executor {
             core-pool-size-min = 2
             core-pool-size-factor = 2.0
             core-pool-size-max = 16
           }
         }
       }

       # configure overrides to ssl-configuration here (to be used by akka-streams, and akka-http – i.e. when serving https connections)
       ssl-config {
         # due to still supporting JDK6 in this release
         # TODO once JDK 8 is required switch this to TLSv1.2 (or remove entirely, leave up to ssl-config to pick)
         protocol = "TLSv1"
       }
    }

    amqp {
      addresses = [
        { host = "192.168.3.171", port = 5672 }
      ],
      virtual-host = "/",
      username = "zulek",
      password = "zulek",
      heartbeat = disable,
      timeout = infinite,
      automatic-recovery = false,
      recovery-interval = 5s
      ssl = disable
    }
                                         """)
}


object mainObj extends App with HttpConfig with DefaultJsonProtocol {

  implicit val system = ActorSystem("zAkkaHttpTest",config)
  implicit val materializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global
  import akka.stream.io._

  val connection = Connection(config)
  val exchange = connection.publish(exchange = "zexchange", routingKey = "zrouting")

  val vkConnection = Http().cachedHostConnectionPoolTls[Int]("api.vk.com")

  val date = DateTime.now()


  def fromVkToBS = Flow[(HttpRequest,Int)]
    .via(vkConnection)
    .map { a =>
      a._1 match {
        case Success(resp) if resp.status == StatusCodes.OK => (resp.entity.dataBytes, a._2)
        case Success(resp) => println("Wrong Status "+ resp.status); (Source.empty[ByteString], a._2)
        case Failure(fa) => println("FAIL " + fa.toString); (Source.empty[ByteString], a._2)
      }
    }
    .map(si => si._1.runFold(ByteString.empty)((a,b) => a ++ b).map(bs => (bs,si._2)))
    .flatMapConcat(f => Source.fromFuture(f))


  val mainStream =

  //Start (id)
  //    Source(1 to 10)
    Source.fromIterator( () => Iterator.continually(ThreadLocalRandom.current().nextInt(100000)) )

    //from (HttpRequest,id) to (ByteString,id)
    .map { id => HttpRequest(uri = s"/method/wall.get?owner_id=$id&count=1&v=5.37") -> id }
    .via(fromVkToBS)
    //from (ByteString,id) to (JSObject,id)
    .map {
      x =>
        (Try(x._1.utf8String.parseJson.asJsObject),x._2)
    }
    .map {
      case (Success(s),i) => (s,i)
      case (Failure(f),i) => println(s"Failed to parse as json: $f"); ("{error: -1}".parseJson.asJsObject,i)
    }

    //from (JSObject,id) to (HttpRequest,(id,offset,count))
    .filter(joi =>
      if (joi._1.fields.head._1 == "response")   {true}
      else {
//        println(joi._1.fields.head._1 +":"+ joi._1.fields.head._2.asJsObject.getFields("error_code","error_msg").mkString(","))
        false
      })
    .mapAsyncUnordered(4)(joi => {
        Future{
          val window = 1000
          val count = joi._1.fields.head._2.asJsObject.fields("count").toString().toInt
          val id = joi._2
          val offsets = (0 to count / window).map { x => x * window }
          val windows = offsets.map((_, window))
          Source(windows).map {
            case (offset, ct) =>
              HttpRequest(uri = s"/method/wall.get?owner_id=$id&offset=$offset&count=$window&v=5.37") -> (id, offset, count)
          }
        }
      }
    )
    .flatMapConcat(a => a)

    //from (HttpRequest,(id,offset,count)) to (ByteString, id, DateTime)
    .map(hr => (hr._1,hr._2._1))
    .via(fromVkToBS)
    .map(wodate => (wodate._1,wodate._2,date))


    //from (B,i,d) to (JsValue)
    .map {
      bid =>
        (bid._1.utf8String.parseJson.asJsObject.getFields("response").find(jv => jv.asJsObject.fields.contains("items")),bid._2,bid._3)
    }
    .map {
      case (Some(s),i,d) =>
        Source(s.asJsObject.fields("items").convertTo[List[JsValue]])

      case (None,i,d) => null
//      println(s"Failed to parse as json"); ("{error: -1}".parseJson.asJsObject,i,d)
    }

    .flatMapConcat(a => a)

    //End (rabbitmq)
//  .runWith(Sink.ignore)
    .map(a => Message(ByteString(a.toString())))
    .runWith(Sink.fromSubscriber(exchange))

//      .runWith(Sink.foreach(a => println(a._2+" "+a._3+" "+a._1.decodeString("UTF-8"))))

}
