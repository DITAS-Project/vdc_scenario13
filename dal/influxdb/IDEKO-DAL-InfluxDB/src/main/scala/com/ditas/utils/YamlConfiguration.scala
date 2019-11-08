package com.ditas.utils

import java.io.FileInputStream

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.reflect.ClassTag

object YamlConfiguration {

  def loadConfiguration[C](filename : String)(implicit ct: ClassTag[C]): C = {
    val yaml = new Yaml(new Constructor(ct.runtimeClass))
    val stream = new FileInputStream(filename)
    try {
      val obj = yaml.load(stream)
      obj.asInstanceOf[C]
    } finally {
      stream.close()
    }
  }
}
