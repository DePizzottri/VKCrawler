import org.scalatest._

import vkcrawler.DataModel.SprayJsonSupport
import spray.json._

import org.joda.time.DateTime

class JsonSupportSpec extends FlatSpec with Matchers {
  "JodaDateTimeSupport " should "have same local time format in (de)marshalling" in {
    import vkcrawler.DataModel.SprayJsonSupport.JodaDateTimeSupport._
    val dt = new DateTime

    val marshalled = dt.toJson
    val umarshalled = marshalled.toJson.convertTo[DateTime]

    println(marshalled.prettyPrint)

    dt.getMillis should be (umarshalled.getMillis)
    dt should be (umarshalled)
  }
}
