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

package scala.compat

import scala.util.Try

private[scala] abstract class ClassValueCompat[T] {

  private def checkClassValueAvailability(): Boolean = Try {
    Class.forName("java.lang.ClassValue", false, getClass.getClassLoader)
    true
  }.getOrElse(false)

  private val instance = if (checkClassValueAvailability()) new JavaClassValue() else new FallbackClassValue()

  private class JavaClassValue extends ClassValue[T] with ClassValueInterface {
    override def computeValue(`type`: Class[_]): T = ClassValueCompat.this.computeValue(`type`)
  }

  private class FallbackClassValue extends ClassValueInterface {
    override def get(param1Class: Class[_]): T = ClassValueCompat.this.get(param1Class)

    override def remove(param1Class: Class[_]): Unit = {}
  }

  private trait ClassValueInterface {
    def get(param1Class: Class[_]): T
    def remove(param1Class: Class[_]): Unit
  }

  def get(`type`: Class[_]): T = instance.get(`type`)

  def remove(`type`: Class[_]): Unit = instance.remove(`type`)

  protected def computeValue(`type`: Class[_]): T

}
