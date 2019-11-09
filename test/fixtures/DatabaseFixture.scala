package fixtures

import java.sql.{Connection, SQLException, Statement}

import com.typesafe.config.Config
import play.api.Logger
import play.api.db.{Database, TransactionIsolationLevel}
import play.api.db.evolutions.Evolutions
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

object DatabaseFixture {
  def apply(config: Config, name: String): DatabaseFixture = new DatabaseFixture(config, name)
  def apply(dbConfigProvider: DatabaseConfigProvider, name: String): DatabaseFixture
    = DatabaseFixture(dbConfigProvider.get[JdbcProfile].config, name)
}

class DatabaseFixture(config: Config, val name: String) extends Database {
  val logger = Logger(this.getClass)
  logger.debug(s"FixtureDatabase '$name' : $url")
  override def url: String = config.getString("db.properties.url")
  override def dataSource = {
    val source = new slick.jdbc.DriverDataSource()
    source.setDriver(config.getString("db.properties.driver"))
    source.setDriverClassName(config.getString("db.dataSourceClass"))
    source.setUrl(url)
    source.setUser(config.getString("db.username"))
    source.setPassword(config.getString("db.password"))
    source
  }

  override def getConnection(): Connection = dataSource.getConnection()

  override def getConnection(autocommit: Boolean): Connection = {
    val connection = getConnection()
    connection.setAutoCommit(autocommit)
    connection
  }

  private def withResource[A, B](resource: A)(cleanup: A => Unit)(doWork: A => B) = try {
    doWork(resource)
  } finally {
    try {
      if (resource != null) {
        cleanup(resource)
      }
    } catch { case e: Throwable =>
      logger.warn(s"withResource cleanup failed: ${e.getMessage}")
    }
  }

  override def withConnection[A](block: Connection => A): A =
    withResource(getConnection())(ccc => ccc.close()) { r =>
      block(r)
    }

  override def withConnection[A](autocommit: Boolean)(block: Connection => A): A =
    withResource(getConnection(autocommit))(_.close) { r =>
      block(r)
    }


  override def withTransaction[A](block: Connection => A): A =
    withResource(getConnection())(_.close()) { connection =>
      val result = block(connection)
      connection.commit()
      result
    }

  override def withTransaction[A](isolationLevel: TransactionIsolationLevel)(block: Connection => A): A =
    withResource(getConnection())(_.close()) { connection =>
      val result = block(connection)
      connection.commit()
      result
    }
  override def shutdown(): Unit = {
  }
  def executeSqlFile(filePath: String, delimiter: String = ";"): DatabaseFixture = {
    val inputStream = getClass.getResourceAsStream(filePath)
    val lines = scala.io.Source.fromInputStream(inputStream).getLines.mkString.split(delimiter)
    executeSql(lines :_*)
  }

  def executeSql(commands: String*): DatabaseFixture = {
    var currentStatement: Statement = null
    commands foreach { command =>
      val rawStatement = command + ";"
      try {
        withTransaction { conn =>
          currentStatement = conn.createStatement
          currentStatement.execute(rawStatement)
        }
      } catch {
        case e: SQLException =>
          logger.warn(s"failed '$rawStatement': ${e.getMessage}")
      } finally {
        if (currentStatement != null) try
          currentStatement.close()
        catch {
          case e: SQLException =>
            e.printStackTrace()
        }
        currentStatement = null
      }
    }
    this
  }

  def evolve(): DatabaseFixture = {
    Evolutions.applyEvolutions(this)
    logger.debug(s"Evolutions evolved")
    this
  }
  def cleanup(): DatabaseFixture = {
    Evolutions.cleanupEvolutions(this)
    logger.debug(s"Evolutions cleaned-up")
    this
  }

}