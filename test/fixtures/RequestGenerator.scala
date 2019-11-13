package fixtures

import model.{Batch, Finish, JobSpecification, Paint}

object RequestGenerator {
  /*
      max-colors=2000
      max-customers=2000
      max-t-values=3000
   */

  def generateSeries(count: Int, nbColors: Int, nbCustomers: Int): Iterator[JobSpecification] =
    ((1 to count) map { _ =>
        generateRequest(nbColors, nbCustomers)
    }).iterator

  def generateExpo(nbColors: Int, nbCustomers: Int): Iterator[JobSpecification] =
    ((1 to nbColors) flatMap { col =>
      (1 to nbCustomers) map { cus =>
        generateRequest(col, cus)
      }
    }).iterator

  def generateRequest(nbColors: Int, nbCustomers: Int): JobSpecification = {
    val demand = (1 to nbCustomers) map { dem =>
      val r = new scala.util.Random
      val code = () => r.nextInt(nbColors) + 1
      val finish = () => Finish(r.nextInt(2))
      val paints = Seq.fill(dem)(Paint(code(), finish()))
      Batch(paints.toArray)
    }
    JobSpecification(nbColors, demand.toArray)
  }
}
