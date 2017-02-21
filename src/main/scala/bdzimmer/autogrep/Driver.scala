// Copyright (c) 2017 Ben Zimmer. All rights reserved.

package bdzimmer.autogrep


object Driver {

  def main(args: Array[String]): Unit = {
    val gui = new GUI(System.getProperty("user.dir"), "scala", "TODO")
  }

}