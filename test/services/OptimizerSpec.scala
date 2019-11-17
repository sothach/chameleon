package services

import algorithm.simple.OptimizerUsingPermutations
import fixtures.TestMetrics
import model._
import org.scalatest.{AsyncFlatSpec, MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar

import scala.language.postfixOps

class OptimizerSpec extends AsyncFlatSpec with MustMatchers with OptionValues with MockitoSugar {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private val color1 = Paint(1)
  private val color2 = Paint(2)
  private val color5 = Paint(5)

  "When job specification is processed, it" should "return the expected solution" in {
    val subject = new OptimizerUsingPermutations(TestMetrics)
    val request = JobSpecification(5, Array(
      Batch(color1.matte),
      Batch(color1.gloss, color2.gloss),
      Batch(color5.gloss)))
    subject.optimize(request) map {
      case Some(MixSolution(batch)) =>
        batch must be(Array(Finish.Matte, Finish.Glossy, Finish.Glossy, Finish.Glossy, Finish.Glossy))
      case None =>
        fail("solution not found")
    }
  }

  "An invalid job specification" should "return the expected solution" in {
    val t = intercept[IllegalArgumentException] {
      JobSpecification(1, Array())
    }
    t.getMessage must include("must be at least one demand (0)")
  }

}
