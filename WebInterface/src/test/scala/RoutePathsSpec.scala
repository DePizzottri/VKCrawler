package com.vkcrawler.WEBInterface

import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.json.AdditionalFormats
import spray.http.StatusCodes

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.joda.time.DateTime

import com.vkcrawler.DataModel.SprayJsonSupport._
import com.vkcrawler.DataModel._

class StubTaskResultProcessor extends TaskResultProcessor{
  var lastProcessed:FriendsListTaskResult = null
  override def process(task: FriendsListTaskResult):Unit = {
    lastProcessed = task
  }
}

object StubTaskResultProcessor {
  val result = FriendsListTaskResult(
      TaskStatistics("stub", new DateTime, new DateTime, new DateTime),
      List()
      )
}

class StubTaskGetter extends TaskGetter {
  override def getTask(types: Array[String]):Either[Task, JsObject] = {
    Left(StubTaskGetter.t)
  }
}

object StubTaskGetter {
  val t = Task("", List(), new DateTime, new DateTime) 
}


import com.vkcrawler.DataModel.SprayJsonSupport.TaskJsonSupport._
import spray.json.DefaultJsonProtocol._

@RunWith(classOf[JUnitRunner])
class RoutePathSpec extends Specification with Specs2RouteTest with ConnectionHandler {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  
  val getter = new StubTaskGetter() 
  val processor = new StubTaskResultProcessor()

  "Service routes" should {
    "response with hello message on /hello page" in {
      Get("/hello") ~> route ~> check {        
        responseAs[String] must contain("Welcome to VC Crawler Task frontend")
      }
    }

    "redirect to /hello page from default request" in {
      Get() ~> route ~> check {
        status === StatusCodes.TemporaryRedirect
        responseAs[String] must contain("""The request should be repeated with <a href="hello">this URI</a>, but future requests can still use the original URI.""")
      }
    }
    
    "returns task JSON object" in  {
      Get("/getTask?version=1&types=stub") ~> route ~> check {
        responseAs[String] == StubTaskGetter.t.toJson.prettyPrint
      }
    }
    
    "correctly pass task result to processor" in  {
    import com.vkcrawler.DataModel.SprayJsonSupport.FriendsListTaskResultJsonSupport._      
      Post("/postTask", StubTaskResultProcessor.result) ~> route ~> check {
        processor.lastProcessed.toString === StubTaskResultProcessor.result.toString &&
        handled && responseAs[String] == "Ok"
      }
    }
  }
}