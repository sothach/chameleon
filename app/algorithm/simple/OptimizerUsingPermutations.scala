package algorithm.simple

import algorithm.BatchOptimizer
import javax.inject.Inject
import model.{Finish, JobSpecification, MixSolution}
import play.api.Logger

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class OptimizerUsingPermutations @Inject()(implicit ec: ExecutionContext) extends BatchOptimizer {
  private val matteFinish = 1
  private val glossFinish = 0
  private case class CustomerFinishes(mattes: Map[Int, Int], glossy: Set[(Int, Int)])
  private object CustomerFinishes {
    val empty: CustomerFinishes = CustomerFinishes(Map.empty, Set.empty)
  }

  def optimize(jobSpec: JobSpecification): Future[Option[MixSolution]] = Future {
    require(jobSpec.demands.flatMap(_.paints).length < 3000,
      "sum of all t-values in request should not exceed 3000")
    val data = jobSpec.demands.map { batch =>
      batch.paints.flatMap(p => Array(p.color, p.finish.id))
    }
    val solution = start(jobSpec, sliceByFinish(data.iterator))
    if (solution.isEmpty) {
      None
    } else {
      val result = MixSolution(solution.map(Finish(_)).toSeq)
      Some(result)
    }
  }

  /*  @tailrec
  def seekSolution(optimal: Seq[Int], perms: Seq[Int], nbCustomers: Int, position: Int,
                   customerFinishes: CustomerFinishes): Seq[Int] = {
    if(solution(optimal, nbCustomers, customerFinishes).nonEmpty) {
      optimal
    } else if (position < optimal.length) {
      // set head to matte and try permutations
      val updated = (matteFinish +: optimal.drop(1)).permutations
      if(updated.hasNext) {
        seekSolution(updated.next, nbCustomers, position + 1, customerFinishes)
      } else {
        Seq.empty
      }
    } else {
      Seq.empty
    }
  }*/


  private def start(jobSpec: JobSpecification, customerFinishes: CustomerFinishes): Array[Int] = {
    var optimal = Seq.fill(jobSpec.colors)(glossFinish)
    var position = 0
    var result = solution(optimal, jobSpec.nbCustomers, customerFinishes)
    while (result.isEmpty && position < optimal.length) {
      position += 1
      val updated = (matteFinish +: optimal.drop(1)).permutations
      while (result.isEmpty && updated.hasNext) {
        optimal = updated.next()
        result = solution(optimal, jobSpec.nbCustomers, customerFinishes)
      }
    }
    result
  }

  @tailrec
  private def sliceByFinish(demands: Iterator[Array[Int]], customer: Int = 0,
                            acc: CustomerFinishes = CustomerFinishes.empty): CustomerFinishes = {
    @tailrec
    def processCustomerDemand(customer: Int, demand: Iterator[Int],
                              acc: CustomerFinishes): CustomerFinishes  = {
      if (demand.hasNext) {
        val color = demand.next()
        val finish = demand.next()
        val parts = if (finish == glossFinish) {
          acc.copy(glossy=acc.glossy + ((customer, color)))
        } else {
          acc.copy(mattes=acc.mattes + (customer -> color))
        }
        processCustomerDemand(customer,demand,parts)
      } else {
        acc
      }
    }
    if (demands.hasNext) {
      val demand = demands.next().iterator
      val parts = if (demand.hasNext) {
        processCustomerDemand(customer,demand,acc)
      } else {
        acc
      }
      sliceByFinish(demands, customer + 1, parts)
    } else {
      acc
    }
  }

  private def solution(finishes: Seq[Int], nbCustomers: Int, cf: CustomerFinishes): Array[Int] = {
    val allCustomersSatisfied = (0 until nbCustomers).forall { batch =>
      finishes.indices.exists { index =>
        finishes(index) match {
          case finish if finish == matteFinish =>
            cf.mattes.contains(batch) && cf.mattes(batch) == index + 1
          case _ =>
            cf.glossy.contains((batch, index + 1))
        }
      }
    }
    if(allCustomersSatisfied) {
      finishes.toArray
    } else {
      Array.empty
    }
  }
}