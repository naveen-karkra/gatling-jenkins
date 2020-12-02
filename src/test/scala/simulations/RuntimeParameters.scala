package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class RuntimeParameters extends Simulation {

  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "30").toInt

  before {
    println(s"Running test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} seconds")
    println(s"Total test duration: ${testDuration} seconds")
  }

  val httpConf = http.baseUrl("http://localhost:8080/app/")
    .header("Accept", "application/json")

  def getAllVideoGames() = {
    exec(
      http("Get all video games")
        .get("videogames")
        .check(status.is(400))
    )
  }

  val scn = scenario("Get all video games")
    .forever() {
      exec(getAllVideoGames())
    }

  setUp(
    scn.inject(
      nothingFor(5 seconds),
      rampUsers(userCount) during (rampDuration second)
    )
  ).assertions(global.responseTime.max.lt(100))     //assertions fail the build in jenkins if the SLA mentioned does not match but the checks does not do that
    .protocols(httpConf)
    .maxDuration(testDuration seconds)

}
