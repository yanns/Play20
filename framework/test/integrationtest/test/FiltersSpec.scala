/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package test

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.Routes
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.duration.Duration
import scala.concurrent._

object FiltersSpec extends Specification with WsTestClient {
  "filters should" should {
    "be able to access request tags" in {

      val notFoundText = "MockGlobal returned 404"
      
      object MockGlobal extends WithFilters(Filter { (f, rh) =>
        Future.successful(rh.tags.get(Routes.ROUTE_VERB).map(verb => Results.Ok(verb)).getOrElse(Results.NotFound(notFoundText)))
      })

      "helpers routing" in new WithApplication(FakeApplication(withGlobal = Some(MockGlobal))) {
        val result = route(FakeRequest("GET", "/")).get
        status(result) must_== 200
        contentAsString(result) must_== "GET"
      }

      "a filter can access request tags" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal))) {
        val response = Await.result(wsCall(controllers.routes.Application.index()).get(), Duration.Inf)
        response.status must_== 200
        response.body must_== "GET"
      }

      "filters fire for every single request, even those not matching a route" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal))) {
        val response = Await.result(wsUrl("/not-a-real-route").get(), Duration.Inf)
        response.status must_== 404
        response.body must_== notFoundText
      }

      object MockGlobal2 extends play.api.GlobalSettings {

        override def doFilter(next: RequestHeader => Handler): (RequestHeader => Handler) = {
          rh => {
            rh.tags.get("ROUTE_CONTROLLER").collect {
              case "controllers.Application" => next(rh)
            }.getOrElse {
              Action {
                Results.BadRequest("Unexpected path!")
              }
            }
          }
        }

      }

      "running server" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal2))) {
        val response = Await.result(wsCall(controllers.routes.Application.plainHelloWorld()).get(), Duration.Inf)
        response.status must_== 200
        response.body must_== ("Hello World")

        val response2 = Await.result(wsCall(controllers.routes.JavaApi.index()).get(), Duration.Inf)
        response2.status must_== 400
        response2.body must_== ("Unexpected path!")
      }
      
      val filterAddedHeaderKey = "CUSTOM_HEADER"
      val filterAddedHeaderVal = "custom header val"
      val filterAddedHeader = new Filter {
        def apply(next: RequestHeader => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
          next(request.copy(headers = addCustomHeader(request.headers)))
        }
        def addCustomHeader(originalHeaders: Headers): Headers = {
          FakeHeaders(originalHeaders.toMap.toSeq :+ (filterAddedHeaderKey -> Seq(filterAddedHeaderVal)))
        }
      }
      
      object MockGlobal3 extends WithFilters(filterAddedHeader) {
        override def onHandlerNotFound(request: RequestHeader) = {
          Future.successful(Results.NotFound(request.headers.get(filterAddedHeaderKey).getOrElse("undefined header")))
        }
      }
        
      "requests not matching a route should receive a RequestHeader modified by upstream filters" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal3))) {
        val response = Await.result(wsUrl("/not-a-real-route").get(), Duration.Inf)
        response.status must_== 404
        response.body must_== filterAddedHeaderVal
      }

      object MockGlobal4 extends WithFilters(filterAddedHeader) {
        override def onError(request: RequestHeader, ex: Throwable): Future[SimpleResult] = {
          Future.successful(Results.NotFound(request.headers.get(filterAddedHeaderKey).getOrElse("undefined header in onError")))
        }
      }

      "requests throwing an internal server error should receive a RequestHeader modified by upstream filters" in new WithServer(FakeApplication(withGlobal = Some(MockGlobal4))) {
        val response = Await.result(wsCall(controllers.routes.Application.syncError()).get(), Duration.Inf)
        response.status must_== 404
        response.body must_== filterAddedHeaderVal
      }
    }
  }
}
