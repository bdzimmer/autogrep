import sbt._
import Keys._

case class JvmSettings(javacSource: String, javacTarget: String, scalacTarget: String)