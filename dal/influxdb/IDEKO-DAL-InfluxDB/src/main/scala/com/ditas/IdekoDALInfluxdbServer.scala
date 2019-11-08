package com.ditas


import java.util.logging.Logger

import com.ditas.configuration.{PrivacyConfiguration, ServerConfiguration}
import com.ditas.ideko.QueryInfluxDBRequest.{QueryInfluxDBGrpc, QueryInfluxDBReply, QueryInfluxDBRequest}
import com.ditas.utils.{JwtValidator, YamlConfiguration}
import com.paulgoldbaum.influxdbclient.InfluxDB
import io.grpc._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.{JavaConverters, mutable}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object IdekoDALInfluxdbServer {
  private val LOGGER = Logger.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: IdekoInfluxdbServer <serverConfigFile> [privacyConfigFile]")
      System.exit(1)
    }
    val configFile = YamlConfiguration.loadConfiguration[ServerConfiguration](args(0))
    if (args.length > 1) {
      validRoles = JavaConverters.asScalaBuffer(YamlConfiguration.loadConfiguration[PrivacyConfiguration](args(1)).validRoles)
    }
    debugMode = configFile.debugMode

    serverConfigFile = configFile
    port = configFile.port
    influxdbServer = configFile.influxdbServer
    influxdbPort = configFile.influxdbPort
    influxdbUsername = configFile.influxdbUsername
    influxdbPassword = configFile.influxdbPassword
    influxdbDBNameMap = configFile.influxdbDBNameMap.asScala
    waitDuration = configFile.waitDuration.seconds

    val server = new IdekoDALInfluxdbServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private var port = 50052 //default port
  private var debugMode = false
  private var influxdbServer = "localhost"
  private var influxdbPort = 8086
  private var influxdbUsername = "username"
  private var influxdbPassword = "password"
  private var influxdbDBNameMap = mutable.Map.empty[String, java.util.ArrayList[String]]
  private var waitDuration = 2.seconds // seconds
  private var serverConfigFile: ServerConfiguration = null

  private var validRoles: mutable.Buffer[String] = ArrayBuffer("*")
}



class IdekoDALInfluxdbServer(executionContext: ExecutionContext) {
  self =>
  private[this] var server: Server = null

  private[this] val queryInfluxDBImpl = new QueryInfluxDBImpl

  private def start(): Unit = {
    val builder = ServerBuilder.forPort(IdekoDALInfluxdbServer.port)
    builder.addService(QueryInfluxDBGrpc.
      bindService(queryInfluxDBImpl, executionContext))
    server = builder.build().start()

    IdekoDALInfluxdbServer.LOGGER.info("Server started, listening on " + IdekoDALInfluxdbServer.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    queryInfluxDBImpl.close()
    if (server != null) {
      server.shutdown()
    }
  }


  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private object QueryInfluxDBImpl {
    private val DB_REGEX = raw"\{\{db\}\}".r
    private val POLICY_REGEX = raw"\{\{policy\}\}".r
  }


  private class QueryInfluxDBImpl extends QueryInfluxDBGrpc.QueryInfluxDB {
    private val influxDB = InfluxDB.connect(IdekoDALInfluxdbServer.influxdbServer, IdekoDALInfluxdbServer.influxdbPort,
      IdekoDALInfluxdbServer.influxdbUsername, IdekoDALInfluxdbServer.influxdbPassword, false, null)
    private val LOGGER = Logger.getLogger(getClass.getName)
    private val jwtValidation = new JwtValidator(IdekoDALInfluxdbServer.serverConfigFile)

    override def query(request: QueryInfluxDBRequest): Future[QueryInfluxDBReply] = {
      val machineId = request.machineId
      val queryObject = request.query
//      val purpose = req.dalMessageProperties.get.purpose

      if (request.dalMessageProperties.isEmpty || request.dalMessageProperties.get.authorization.isEmpty) {
        Future.failed(Status.ABORTED.augmentDescription("Missing authorization").asRuntimeException())
      } else if (queryObject.isEmpty) {
        Future.failed(Status.ABORTED.augmentDescription("Missing query").asRuntimeException())
      } else if (machineId.isEmpty) {
        Future.failed(Status.ABORTED.augmentDescription("Missing machine id").asRuntimeException())
      } else {
        val authorizationHeader: String = request.dalMessageProperties.get.authorization
        try {
          jwtValidation.validateJwtToken(authorizationHeader, IdekoDALInfluxdbServer.serverConfigFile.jwtServerTimeout, IdekoDALInfluxdbServer.validRoles)
        } catch {
          case e: Exception => {
            LOGGER.throwing(getClass.getName, "query", e);
            return Future.failed(Status.ABORTED.augmentDescription(e.getMessage).asRuntimeException())
          }
        }

        val (dbName, policy) = chooseMachine(machineId)
        (dbName, policy) match {
          case (Some(dbName), Some(policy)) => {
            val resultRecords = queryInfluxDB(queryObject, machineId, dbName, policy)
            val reply = new QueryInfluxDBReply(Seq(resultRecords))
            return Future.successful(reply)
          }
          case (None, None) => {
            return Future.failed(Status.ABORTED.augmentDescription("Machine id didn't match any dbName and policy").asRuntimeException())
          }
        }
      }
    }



    private def queryInfluxDB(query: String, machineId: String, dbName: String, policy: String): String = {
      LOGGER.info(s"Query: {${query}}\nmachineId=${machineId}, dbName=${dbName}")
//      val databases = Await.result(influxDB.showDatabases(), IdekoDALInfluxdbServer.waitDuration)
//      println(databases.toList)
      val database = influxDB.selectDatabase(dbName)
      val dbExists = Await.result(database.exists(), IdekoDALInfluxdbServer.waitDuration) // => Future[Boolean]
      LOGGER.info(s"Does DB $dbName exist? $dbExists")

      val queryDBResolved = QueryInfluxDBImpl.DB_REGEX.replaceAllIn(query, dbName)
      LOGGER.info(s"Query with DB name resolved: ${queryDBResolved}")
      val queryPolicyResolved = QueryInfluxDBImpl.POLICY_REGEX.replaceAllIn(queryDBResolved, policy)
      LOGGER.info(s"Query with policy resolved: ${queryPolicyResolved}")
      val queryResult = Await.result(database.queryToJson(queryPolicyResolved), IdekoDALInfluxdbServer.waitDuration)
      LOGGER.info(s"Query result: ${queryResult}")
      queryResult
    }

    def close() = {
      influxDB.close()
    }

    private def chooseMachine(machineId: String): (Option[String], Option[String]) = {
      val dbNameMap = IdekoDALInfluxdbServer.influxdbDBNameMap
      val dbPair = dbNameMap get machineId
      dbPair match {
        case Some(dbPair) => return (Some(dbPair.get(0)), Some(dbPair.get(1)))
        case None => return (None, None)
      }
    }
  }

}





