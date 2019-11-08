package com.ditas.configuration

import scala.beans.BeanProperty

class PrivacyConfiguration {
  @BeanProperty var validRoles = new java.util.ArrayList[String]()
}
