package services

import model.{Batch, Paint, Finish, JobSpecification, MixSolution}

import scala.collection.immutable

class PaintShopPlanner {

  def solve(request: JobSpecification): Option[MixSolution] = {
/*    seekSolution(request) match {
      case Some(plan) =>
        plan.foreach { p => print(p._2 + " ") }
        println
      case None =>
        println("No solution exists")
    }*/
    val color1 = Paint(1)
    Some(
      MixSolution.withPaints(
          Array(color1.matte,color1.gloss,color1.gloss,color1.gloss,color1.gloss)))
  }
/*

  val GLOSS = 0
  val MATTE = 1
  private def seekSolution(request: JobSpecification): Option[List[(Int, Int)]] = {

    // this is our preferred production color run:
    val optimal = request.colors

    // get any customer requirements missing from the optimal run
    val missing = for {
      order <- request.colors
      reqs <- order ^ optimal
    } yield reqs

    // merge the lists, with the customer needs taking precedence (RH term)
    val solution = optimal //++ missing

    // Check that for each customer, there is at least one color they like
    val solved = false //equest.demands.forall(_.exists(solution.contains))

    if (solved) {
      Some(solution.toList.sortBy(_._1))
    } else {
      None
    }
  }

  /* extend the map class with helper functions for this problem */
  private implicit class ExtMap[K, V](map: Map[K, V]) {

    /** @return true if this map contains the key/value mapping supplied */
    def contains(item: (K, V)): Boolean =
      map.get(item._1) match {
        case Some(v) => v == item._2
        case _ => false
      }

    /** symmetric difference (xor) treating mapped pairs as set members
     * @return this map if it does not intersect other, else Nil */
    def ^(other: Map[K, V]): immutable.Iterable[(K, V)] with PartialFunction[K with Int, V] =
      if (map.exists(other.contains(_))) {
        Nil
      } else {
        map
      }
  }
*/

}
