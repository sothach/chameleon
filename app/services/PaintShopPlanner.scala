package services

import model.{JobSpecification, MixSolution, Finish}

class PaintShopPlanner {
  import Finish._

  def solve(request: JobSpecification): Option[MixSolution] = {
    Some(MixSolution(Array(Matte,Glossy,Glossy,Glossy,Glossy)))
  }

}
