package services

import algorithm.simple.OptimizerUsingPermutations
import model._
import org.scalatest.{AsyncFlatSpec, MustMatchers, OptionValues}

import scala.language.postfixOps

class PlannerSpec extends AsyncFlatSpec with MustMatchers with OptionValues {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  /*
  Integer N, the number of paint colors,
  integer M, the number of customers.
  A list of M lists, one for each customer, each containing:
    An integer T >= 1, the number of paint types the customer likes,
    followed by T pairs of integers "X Y", one for each type the customer likes,
    where X is the paint color between 1 and N inclusive,
    and Y is either 0 to indicate glossy, or 1 to indicated matte.
    Note that: No pair will occur more than once for a single customer.
    Each customer will have at least one color that they like (T >= 1).
    Each customer will like at most one matte color.
    (At most one pair for each customer has Y = 1).

    {
      "colors": 1,
      "customers": 2,
      "demands": [
        [1, 1, 1],
        [1, 1, 0]
      ]
    }
    => 1 0 0 0 0
    http://0.0.0.0:8080/v1/?input={"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}
   */

  "When job specification is processed, it" should "return the expected solution" in {
    val subject = new OptimizerUsingPermutations()
    val color1 = Paint(1)
    val color2 = Paint(2)
    val color5 = Paint(5)
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

}
