package algorithm

import algorithm.simple.OptimizerUsingPermutations
import model._
import org.scalatest.{AsyncFlatSpec, FlatSpec, Matchers, OptionValues}

class BatchSpec extends AsyncFlatSpec with Matchers with OptionValues {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  private val subject = new OptimizerUsingPermutations

  import Finish._
  import subject.optimize

  "A request specification" should "be solved" in {
    val jobSpec = JobSpecification.build(2, Batch(Paint(1).matte), Batch(Paint(1).gloss, Paint(2).matte))
    subject.optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Matte, Matte))
    }
  }

  "A batch with too few colors available to produce the customers demand for both Matte and Gloss of the same color" should
    "result in an empty solution" in {
    val jobSpec = JobSpecification.build(1, Batch(Paint(1).matte), Batch(Paint(1).gloss))
    optimize(jobSpec) map { result =>
      result should be (empty)
    }
  }

  "A batch with too few colors available to produce the customers demands" should
    "result in tan empty" in {
    val jobSpec = JobSpecification.build(1, Batch(Paint(1).matte), Batch(Paint(2).matte))
    optimize(jobSpec) map { result =>
      result should be(empty)
    }
  }

  "A batch with two demands for different colors both gloss" should
    "result in the string 0 0 (ie mix both colors, using gloss)" in {
    val jobSpec = JobSpecification.build(2, Batch(Paint(1).gloss), Batch(Paint(2).gloss))
    optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Glossy, Glossy))
    }
  }

  "A batch with 3 customers with 1,2 and 3 color requirements, where the only way it can be satisfied is with matte" should
    "result in the string 1 1 1 (ie mix all three colors, using matte)" in {
    val jobSpec = JobSpecification.build(3,
      Batch(Paint(1).matte), Batch(Paint(1).gloss, Paint(2).matte), Batch(Paint(1).gloss, Paint(2).gloss, Paint(3).matte))
    subject.optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Matte, Matte, Matte))
    }
  }

  "A batch with 3 customers with 3 color requirements" should
    "result in the string 0 0 0 0 1 (ie mix all the colors in the cheaper gloss)" in {
    val jobSpec = JobSpecification.build(5, Batch(Paint(5).matte), Batch(Paint(1).gloss, Paint(2).matte))
    subject.optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Glossy, Glossy, Glossy, Glossy, Matte))
    }
  }

  "A batch with 5 customers with 5 color requirements" should
    "result in the string 1 0 1 0 0" in {
    val jobSpec = JobSpecification.build(5,
      Batch(Paint(1).matte),
      Batch(Paint(1).gloss, Paint(2).gloss),
      Batch(Paint(5).gloss),
      Batch(Paint(1).matte),
      Batch(Paint(3).matte)
    )
    subject.optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Matte, Glossy, Matte, Glossy, Glossy))
    }
  }

  "A batch with 2 customers with 2 color requirements" should
    "result in the string 1 1" in {
    val jobSpec = JobSpecification.build(2, Batch(Paint(1).matte), Batch(Paint(1).gloss, Paint(2).matte))
    subject.optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Matte, Matte))
    }
  }

  "A batch with 6 customers with 5 color requirements" should
    "result in the string 0 0 0 1 1" in {
    val jobSpec = JobSpecification.build(5,
      Batch(Paint(1).matte, Paint(2).gloss, Paint(4).gloss),
      Batch(Paint(2).matte, Paint(1).gloss),
      Batch(Paint(2).matte, Paint(3).gloss),
      Batch(Paint(3).matte, Paint(2).gloss),
      Batch(Paint(4).matte, Paint(5).gloss),
      Batch(Paint(5).matte)
    )
    subject.optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Glossy, Glossy, Glossy, Matte, Matte))
    }
  }

  "A batch with 6 customers with 5 color requirements" should
    "result in a list and hash map with the specified values" in {
    val jobSpec = JobSpecification.build(5,
      Batch(Paint(1).matte, Paint(2).gloss, Paint(4).gloss),
      Batch(Paint(2).matte, Paint(1).gloss),
      Batch(Paint(3).gloss),
      Batch(Paint(3).matte, Paint(2).gloss),
      Batch(Paint(4).matte, Paint(5).gloss),
      Batch(Paint(5).matte)
    )
    subject.optimize(jobSpec) map { result =>
      result.value should be(MixSolution.of(Glossy, Glossy, Glossy, Matte, Matte))
    }
  }

}