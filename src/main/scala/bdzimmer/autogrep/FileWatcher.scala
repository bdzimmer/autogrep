// Copyright (c) 2017 Ben Zimmer. All rights reserved.

package bdzimmer.autogrep

import scala.collection.JavaConversions._

import java.io.File

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.commons.io.filefilter.FileFilterUtils

import org.apache.commons.io.monitor.{FileAlterationListenerAdaptor, FileAlterationMonitor, FileAlterationObserver}


class FileWatcher(
    baseDir: File,
    extensions: Set[String],
    interval: Int,
    fileSearcher: FileSearcher) {

  var fileSet: Set[File] = Set()
  updateFileSet()

  var filtersWIP = FileFilterUtils.directoryFileFilter()
  extensions.foreach(ext => {
    filtersWIP = FileFilterUtils.or(filtersWIP,
        FileFilterUtils.and(FileFilterUtils.fileFileFilter, FileFilterUtils.suffixFileFilter("." + ext)))
  })
  val filters = filtersWIP

  val observer = new FileAlterationObserver(baseDir.getAbsolutePath, filters)
  val monitor = new FileAlterationMonitor(interval)

  val listener = new FileAlterationListenerAdaptor() {

    override def onFileChange(file: File): Unit = {
      println("FILE CHANGED - " + file.getAbsolutePath)
      if (fileSet.contains(file)) {
        fileSearcher.add(file)
      }
    }

    override def onFileCreate(file: File): Unit = {
      val ext = FilenameUtils.getExtension(file.getName)
      if (extensions.contains(ext)) {
        fileSet = fileSet + file
        fileSearcher.add(file)
      }
    }

    override def onFileDelete(file: File): Unit = {
      val ext = FilenameUtils.getExtension(file.getName)
      if (extensions.contains(ext)) {
        fileSet = fileSet - file
        fileSearcher.remove(file)
      }
    }

    override def onDirectoryChange(dir: File): Unit = {
      updateFileSet()
    }

    override def onDirectoryCreate(dir: File): Unit = {
      updateFileSet()
    }

    override def onDirectoryDelete(dir: File): Unit = {
      updateFileSet()
    }

    override def onStart(observer: FileAlterationObserver): Unit = {
      println("start")
    }

    override def onStop(observer: FileAlterationObserver): Unit = {
      println("stop")
    }
  }

  observer.addListener(listener)
  monitor.addObserver(observer)
  monitor.start()


  def buildFileSet(): Set[File] = {
    FileUtils.listFiles(baseDir, extensions.toArray, true).toSet
  }


  def updateFileSet(): Unit = {

    val updatedFileSet = buildFileSet()

    // TODO: these might be switched
    val addedFiles = updatedFileSet.diff(fileSet)
    val removedFiles = fileSet.diff(updatedFileSet)

    addedFiles foreach fileSearcher.add
    removedFiles foreach fileSearcher.remove

    fileSet = updatedFileSet

  }

}
