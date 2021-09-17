/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala.runtime

import java.io.Serializable
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import compat.ClassValueCompat

private[runtime] object ModuleSerializationProxy {
  private val instances = new ClassValueCompat[Object] {
    override protected def computeValue(`type`: Class[_]): Object = {
      try {
        java.security.AccessController.doPrivileged(new PrivilegedExceptionAction[Object] {
          override def run(): Object = {
            `type`.getField("MODULE$").get(null)
          }
        })
      } catch {
        case e: PrivilegedActionException =>
          rethrowRuntime(e.getCause)
      }
    }
  }

  private def rethrowRuntime(e: Throwable): Object = {
    val cause = e.getCause
    cause match {
      case exception: RuntimeException => throw exception
      case _ => throw new RuntimeException(cause)
    }
  }
}

@SerialVersionUID(1L)
final class ModuleSerializationProxy(moduleClass: Class[_]) extends Serializable {
  @SuppressWarnings(Array("unused")) private def readResolve = ModuleSerializationProxy.instances.get(moduleClass)
}
