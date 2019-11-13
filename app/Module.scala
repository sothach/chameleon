import algorithm.BatchOptimizer
import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    val optimizerClass: Class[_ <: BatchOptimizer] =
      environment.classLoader
        .loadClass(configuration.get[String]("optimizer.implementation"))
        .asSubclass(classOf[BatchOptimizer])
    bind(classOf[BatchOptimizer]).to(optimizerClass)
  }
}