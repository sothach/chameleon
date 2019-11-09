package services

import model.Finish.{Glossy, Matte}
import model.{JobSpecification, MixSolution}
import org.scalatest.{FlatSpec, MustMatchers}

import scala.language.postfixOps

class PlannerSpec extends FlatSpec with MustMatchers {

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

  "When an existing job is queried, it" should "be returned" in {
    val subject = new PaintShopPlanner()
    val request = JobSpecification(5, 3, Array(Array(1,1,1),Array(2,1,0,2,0),Array(1,5,0)))
    val solution = Some(MixSolution(Array(Matte,Glossy,Glossy,Glossy,Glossy)))
    val result = subject.solve(request)
    result match {
      case Some(MixSolution(batch)) =>
        batch must be(Array(Matte,Glossy,Glossy,Glossy,Glossy))
      case None =>
        fail("solution not found")
    }
  }

}
