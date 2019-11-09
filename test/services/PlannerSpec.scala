package services

import model.{Batch, Color, JobSpecification, MixSolution}
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}

import scala.language.postfixOps

class PlannerSpec extends FlatSpec with MustMatchers with OptionValues {

  /*
  Integer N, the number of paint colors,
  integer M, the number of customers.
  A list of M lists, one for each customer, each containing:
    An integer T >= 1, the number of paint types the customer likes,
    followed by T pairs of integers "X Y", one for each type the customer likes,
    where X is the paint color between 1 and N inclusive,
    and Y is either 0 to indicate glossy,
    or 1 to indicated matte.
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
1 0 0 0 0
    http://0.0.0.0:8080/v1/?input={"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}
   */

  "When job specification is processed, it" should "return the expected solution" in {
    val subject = new PaintShopPlanner()
    val color1 = Color(1)
    val color2 = Color(2)
    val color5 = Color(5)
    val request = JobSpecification(Array(
        Batch(Array(color1.matte)),
        Batch(Array(color1.gloss,color2.gloss)),
        Batch(Array(color5.gloss))
      ))
    val result = subject.solve(request)
    result match {
      case Some(MixSolution(batch)) =>
        batch.colors must be(Array(color1.matte,color1.gloss,color1.gloss,color1.gloss,color1.gloss))
      case None =>
        fail("solution not found")
    }
  }

}
