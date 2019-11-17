package fixtures

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics

object TestMetrics extends Metrics {
  override def defaultRegistry: MetricRegistry = new MetricRegistry

  override def toJson: String = "{}"
}
