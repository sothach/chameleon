package algorithm
import algorithm.simple.OptimizerUsingPermutations
import model._
import org.scalatest.{FlatSpec, Matchers, OptionValues}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LegacyTestSuite extends FlatSpec with Matchers with OptionValues {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  info("Execute the existing set of tests, from the provided files")
  private val subject = new OptimizerUsingPermutations

  "The algorithm" should
    "return the expected results for the set of test cases provided" in {
    @tailrec
    def getExpected(output: Iterator[String], caseNb: Int = 1,
                    acc: Map[Int, String] = Map.empty): Map[Int, String] =
      if (output.hasNext) {
        getExpected(output, caseNb + 1, acc + (caseNb -> output.next))
      } else {
        acc
      }
    @tailrec
    def readRequests(caseNb: Int, requests: Iterator[String])(check: (Int, Int, List[String]) => Int): Int = {
      if (requests.hasNext) {
        val nbColors = requests.next.toInt
        val nbCustomers = requests.next.toInt
        val demands = requests.take(nbCustomers)
        check(caseNb + 1, nbColors, demands.toList)
        readRequests(caseNb + 1, requests)(check)
      } else {
        caseNb
      }
    }

    val expected = getExpected(streamDataFile("unit-tests.out.txt"))
    val testCases = streamDataFile("unit-tests.in.txt")
    val nbTestCases = testCases.next.toInt
    val testCount = readRequests(0, testCases) {
      (caseNb: Int, colors: Int, demands: List[String]) => {
        val orders = demands.map(parseCase) map (order => Batch(order.toArray))
        val jobSpec = JobSpecification(colors, orders.toArray)
        val result = Await.result(subject.optimize(jobSpec), Duration.Inf)
        val solution = renderResult(result)
        s"Case #$caseNb: $solution" should be(expected(caseNb))
        caseNb
      }
    }
    testCount should be(nbTestCases)
  }

  private def parseCase(orderLine: String): List[Paint] = {
    val line = orderLine.trim.split(" ")
    require(line.length >= 3)
    line.drop(1).sliding(2, 2).map { spec =>
      require(spec.length == 2)
      Paint(spec.head.toInt, Finish(spec.last.toInt))
    }.toList
  }

  private def renderResult(result: Option[MixSolution]) = result match {
    case Some(solution) => solution.legacyFormat
    case None => "IMPOSSIBLE"
  }

  private def streamDataFile(resource: String): Iterator[String] = {
    val stream = getClass.getResourceAsStream(s"/$resource")
    scala.io.Source.fromInputStream(stream).getLines()
  }
}