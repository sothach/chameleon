package algorithm

import org.scalatest.{FlatSpec, Matchers}

import scala.io.BufferedSource

class AlgorithmSpec extends FlatSpec with Matchers {
  import PaintShopERP._

  "A request specification" should "be solvedd" in {
    val case3_5 = Seq("1 1 1","2 1 0 2 0","1 5 0","1 1 1","1 3 1")
    val expect = "1 0 1 0 0"

    val orders = case3_5.toList.map (parseOrder)
    val solution = makeBatch ( 5, orders)
    solution should be(expect)
  }


  "A batch of requests" should "result in expected" in {
    @scala.annotation.tailrec
    def getExected(output: Iterator[String], caseNb: Int = 1,
                   acc: Map[Int,String] = Map.empty): Map[Int,String] = {
      if(output.hasNext) {
        val map = acc + (caseNb -> output.next)
        getExected(output, caseNb+1,map)
      } else {
        acc
      }
    }
    val expected = getExected(streamDataFile("unit-tests.out.txt"))
    val inputs = streamDataFile("unit-tests.in.txt")
    @scala.annotation.tailrec
    def readrequest(caseNb: Int, requests: Iterator[String])(check: (Int,Int,Int,List[String]) => Int): Int = {
      if(requests.hasNext) {
        val nbColors = requests.next.toInt
        val nbCustomers = requests.next.toInt
        val specs = requests.take(nbCustomers)
        check(caseNb+1,nbColors, nbColors, specs.toList)
        readrequest(caseNb+1,requests)(check)
      } else {
        caseNb
      }
    }
    val checker = (caseNb: Int, colors: Int, customers: Int, data: List[String]) => {
      val orders = data.map (parseOrder)
      val solution = makeBatch ( colors, orders)
      //solution should be(expect)
      println(s"Case #$caseNb: $solution")
      s"Case #$caseNb: $solution" should be(expected(caseNb) )
      caseNb
    }
    val nbCases = inputs.next.toInt
    val executed = readrequest(0, inputs)(checker)
    nbCases should be(executed)
  }

  private def streamDataFile(resource: String): Iterator[String] = {
    val stream = getClass.getResourceAsStream(s"/$resource")
    scala.io.Source.fromInputStream(stream).getLines()
  }
}