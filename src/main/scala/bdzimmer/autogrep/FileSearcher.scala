// Copyright (c) 2017 Ben Zimmer. All rights reserved.

package bdzimmer.autogrep

import scala.collection.mutable.{Buffer, Map => MutableMap}
import scala.util.regexp

import java.io.File

import org.apache.commons.io.FileUtils


case class Match(
  val file: File,
  val filename: String,
  val lineNumber: Int,
  val line: String
)


class FileSearcher(
    baseDir: File,
    matchString: String,
    update: List[Match] => Unit) {

  val matcher = matchString.r

  val dict: MutableMap[File, List[Match]] = MutableMap()


  def add(file: File): Unit = {
    dict(file) = search(file)
    refresh()
  }


  def remove(file: File): Unit = {
    dict.remove(file)
    refresh()
  }


  private def search(file: File): List[Match] = {
    val matches: Buffer[Match] = Buffer()
    var lineNumber = 1
    val lines = FileUtils.lineIterator(file)
    while(lines.hasNext) {
      val line = lines.next()
      if (matcher.findFirstIn(line).isDefined) {
        val filename = baseDir.toURI.relativize(file.toURI).getPath
        matches += Match(file, filename, lineNumber, line)
      }
      lineNumber += 1
    }
    matches.toList
  }


  private def refresh(): Unit = {
    val matches = dict.flatMap(x => x._2).toList
    update(matches)
  }

}
