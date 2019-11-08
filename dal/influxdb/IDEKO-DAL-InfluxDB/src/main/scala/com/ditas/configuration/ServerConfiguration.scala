package com.ditas.configuration

import java.util

import scala.beans.BeanProperty

class ServerConfiguration {
  @BeanProperty var debugMode: Boolean = false
  @BeanProperty var port: Integer = null

  @BeanProperty var influxdbServer: String = "localhost"
  @BeanProperty var influxdbPort: Integer = 8086
  @BeanProperty var influxdbUsername: String = "username"
  @BeanProperty var influxdbPassword: String = "password"
  @BeanProperty var influxdbDBNameMap: util.Map[String, util.ArrayList[String]] = new util.HashMap[String, util.ArrayList[String]]()
  @BeanProperty var waitDuration: Int = 2 // seconds

  @BeanProperty var jwtServerTimeout: Int = 5000 // milliseconds
  @BeanProperty var jwksServerEndpoint: String = null
  @BeanProperty var jwksCheckServerCertificate: Boolean = true

}
