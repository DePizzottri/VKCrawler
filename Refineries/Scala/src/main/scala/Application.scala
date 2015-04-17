import akka.actor._
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import spray.routing.HttpService
import spray.can._

case class Schedule(val duration:FiniteDuration)
case class Suspend()
case class Resume()
case class Shutdown()
case class Work()
case class GetName()

class Worker extends Actor with ActorLogging {
  def receive = {
    case Work => {
      log.info("Working start")
      Thread.sleep(5000)
      log.info("Working finished")
    }
    case GetName => sender ! "SimpleWorker"
  }
}

class ServiceActor(worker:ActorRef) extends Actor with ActorLogging {
  lazy val name:Future[String] = {
      implicit val timeout = Timeout(5 seconds)
      (worker ? GetName).mapTo[String]
  }
  
  def stop: Receive = {
    case Shutdown => context.system.shutdown()
    case GetName => {
      import scala.concurrent.ExecutionContext.Implicits.global
      pipe(name) to sender
    }
  }
  
  def receive = stop orElse {
    case Schedule(d) => context.become(start(d), true)
  }  
  
  def start(duration: FiniteDuration) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val c = context.system.scheduler.schedule(duration, duration, worker, Work)
    working(duration, c)
  }
  
  def working(duration: FiniteDuration, prevWork: Cancellable): Receive = stop orElse {
    case msg@Schedule(d) => prevWork.cancel(); context.become(start(d), true)
    case Suspend => prevWork.cancel(); context.become(suspended(duration) orElse stop)
  }
  
  def suspended(duration: FiniteDuration): Receive = stop orElse {
    case s:Schedule => context.unbecome(); self ! s
    case Resume => context.unbecome(); self ! Schedule(duration)
  }
}

class WebService(val service: ActorRef) extends Actor with HttpService {
  def actorRefFactory = context
  def receive = runRoute(route)

  val route = {
    path("") {
      get {
        complete("Service web interface")
      }
    } ~    
    path("suspend") {
      get { 
        complete {
          service ! Suspend
          "OK"
        }
      }
    } ~ 
    path("resume") {
      get {
        complete {
          service ! Resume
          "OK"
        }
      }
    } ~
    path("shutdown") {
      get {
        complete {
          service ! Shutdown
          "OK"
        }
      }
    } ~ 
    path("schedule") {
      get {
        parameters("duration".as[Int]) { duration =>
          complete {
            service ! Schedule(duration.seconds)
            "OK"
          }
        }
      }
    } ~
    path("name") {
      get {
        implicit val timeout = Timeout(5 seconds)
        val f = (service ? GetName).mapTo[String]
        import scala.concurrent.ExecutionContext.Implicits.global
        onSuccess(f) { name =>
          complete(name)
        }
      }
    }
  }
}

object Application extends App {
  implicit val system = ActorSystem("test-system")

  val worker = system.actorOf(Props(classOf[Worker]), "WorkerActor")
  val commandActor = system.actorOf(Props(classOf[ServiceActor], worker), "TestActor")
  val webActor = system.actorOf(Props(classOf[WebService], commandActor), "WebServiceActor")
  
  IO (Http) ! Http.Bind(webActor, "0.0.0.0", 8080)  
  
  system.awaitTermination()
}