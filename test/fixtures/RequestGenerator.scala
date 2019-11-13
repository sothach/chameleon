package fixtures

import model.{Batch, Finish, JobSpecification, Paint}

object RequestGenerator {

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
    val r = new scala.util.Random
    val demands = (1 to nbCustomers) map { dem =>
      val paint = () => Paint(r.nextInt(nbColors) + 1,  Finish(r.nextInt(2)))
      val paints = Seq.fill(nbColors)(paint())
      Batch(paints.toArray)
    }
    JobSpecification(nbColors,  demands.toArray)
  }
}
