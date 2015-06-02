package com.vkcrawler.WEBInterface

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import spray.http.StatusCodes

@RunWith(classOf[JUnitRunner])
class RouteSpec extends Specification with Specs2RouteTest with ConnectionHandler {
  def actorRefFactory = system // connect the DSL to the test ActorSystem

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
  }
}