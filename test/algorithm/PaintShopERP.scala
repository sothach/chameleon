package algorithm

object PaintShopERP {

  /* extend the map class with helper functions for this problem */
  implicit class ExtMap[K, V](map: Map[K, V]) {

    /** @return true if this map contains the key/value mapping supplied */
    def contains(item: (K, V)): Boolean =
      map.get(item._1) match {
        case Some(v) => v == item._2
        case _ => false
      }

    /** symmetric difference (xor) treating mapped pairs as set members
     *
     * @return this map if it does not intersect other, else Nil */
    def ^(other: Map[K, V]) =
      if (map.exists(other.contains(_))) {
        Nil
      } else {
        map
      }
  }

  val GLOSS = 0
  val MATTE = 1

  /**
   * @param nbColors number of colors in the production run
   * @param orders   for paints placed by customers
   * @return an option on the color-run that satisfies the customer
   *         orders and the paint shop's production constraints, as a
   *         list of finishes (G or M) sorted by color number
   */
  def solve(nbColors: Int,
            orders: List[Map[Int, Int]]): Option[List[(Int, Int)]] = {


    assert(nbColors > 0, "valid color run")
    assert(orders.forall(_.isEmpty == false), s"non-empty order lines: $orders")

    // this is our preferred production color run:
    val optimal = (for {color <- 1 to nbColors}
      yield (color -> GLOSS)).toMap

    // get any customer requirements missing from the optimal run
    val missing = for {
      order <- orders
      reqs <- order ^ optimal
    } yield reqs

    // merge the lists, with the customer needs taking precedence (RH term)
    val solution = optimal ++ missing

    // Check that for each customer, there is at least one color they like
    val solved = orders.forall(_.exists(solution.contains(_)))

    if (solved) {
      Some(solution.toList.sortBy(_._1))
    } else {
      None
    }
  }

  def makeBatch(nbcol: Int,
                data: List[Map[Int, Int]]): String = {
    solve(nbcol, data) match {
      case Some(plan) =>
        plan.map(_._2).mkString(" ")
      case None =>
        "IMPOSSIBLE"
    }
  }


  private val InputFormat = """(\d+) ([01])""".r

  /** parse the supplied order line
   *
   * @param orderLine a sequence of color number and finishes,
   *          e.g., "1 1 3 0 5 0", representing color 1 in matte,
   *                  color 3 in gloss and color 5 in gloss
   * @return a map of colors to finishes, representing a customer order */
  def parseOrder(orderLine: String): Map[Int, Int] = {
    val orders = for {
      InputFormat(color, finish) <- InputFormat findAllIn orderLine
    } yield (color.toInt, finish.toInt)
    orders.toMap
  }

}