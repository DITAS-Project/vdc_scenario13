package com.paulgoldbaum.influxdbclient

protected[influxdbclient]
object Util {
  def escapeString(str: String) = str.replaceAll("([ ,=])", "\\\\$1")
}
