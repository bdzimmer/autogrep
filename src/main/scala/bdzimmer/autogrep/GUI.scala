// Copyright (c) 2017 Ben Zimmer. All rights reserved.

package bdzimmer.autogrep

import scala.sys.process._

import java.io.File

import java.awt.{BorderLayout, Component, Dimension, Font, GridLayout, LayoutManager}
import java.awt.event.{ActionEvent, ActionListener, MouseAdapter, MouseEvent}

import javax.swing.{JFrame, JTable, JScrollPane, JTextField, JPanel, JLabel, JButton}
import javax.swing.event.{ListSelectionEvent, ListSelectionListener}
import javax.swing.table.{DefaultTableModel, TableRowSorter, TableCellRenderer}

import org.apache.commons.io.FilenameUtils


class GUI(
  baseDir: String,
  exts: String,
  regex: String) extends JFrame {

  var watcher: Option[FileWatcher] = None

  // results table

  val EmptyContent: Array[Array[Object]] = Array(Array())
  val ColumnNames: Array[Object] = Array("Directory", "File", "Line", "Match")

  val model = new DefaultTableModel(EmptyContent, ColumnNames) {
    override def isCellEditable(row: Int, column: Int): Boolean = false
  }

  val table = new JTable(model) {
    // http://stackoverflow.com/questions/17858132/automatically-adjust-jtable-column-to-fit-content
    override def prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component = {
      val component = super.prepareRenderer(renderer, row, column)
      val rendererWidth = component.getPreferredSize().width
      val tableColumn = getColumnModel().getColumn(column)
      tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()))
      component
    }
  }

  table.addMouseListener(new MouseAdapter() {
    override def mousePressed(me: MouseEvent): Unit = {
      if (me.getClickCount == 2) {
        val baseDirname = new File(dirField.getText)
        val rowIdx = table.getSelectedRow
        val dir = table.getValueAt(rowIdx, 0).toString
        val filename = baseDirname + "/" + dir + "/" + table.getValueAt(rowIdx, 1).toString
        val lineNumber = Integer.parseInt(table.getValueAt(rowIdx, 2).toString)
        val command = s"""notepad++ "${filename}" -n${lineNumber}"""
        println(command)
        command.!!
      }
    }
  });

  table.setShowGrid(false)
  table.setFont(new Font("Consolas", Font.PLAIN, 11))
  table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
  table.setAutoCreateRowSorter(true)

  val scrollPane = new JScrollPane(table)
  scrollPane.setPreferredSize(new Dimension(800, 600))

  // controls

  val controlPanel = new JPanel(new BorderLayout())

  val labels = new JPanel(new GridLayout(4, 1))
  val controls = new JPanel(new GridLayout(4, 1))

  labels.add(new JLabel("Directory"))
  val dirField = new JTextField(baseDir)
  controls.add(dirField)

  labels.add(new JLabel("Extensions"))
  val extsField = new JTextField(exts)
  controls.add(extsField)

  labels.add(new JLabel("Regex"))
  val regexField = new JTextField(regex)
  controls.add(regexField)

  val searchButton = new JButton("Search")
  searchButton.addActionListener(new ActionListener() {
    def actionPerformed(ae: ActionEvent): Unit = {
      initWatcher()
    }
  })
  controls.add(searchButton)
  controlPanel.add(labels, BorderLayout.WEST)
  controlPanel.add(controls, BorderLayout.CENTER)

  // build frame

  setTitle("autogrep")
  setLayout(new BorderLayout())
  add(controlPanel, BorderLayout.NORTH)
  add(scrollPane, BorderLayout.CENTER)
  pack()
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  setVisible(true)

  ////

  def initWatcher(): Unit = {

    val baseDir = new File(dirField.getText)
    val extensions = extsField.getText.split("\\s+").toSet
    val pattern = regexField.getText

    setTitle("autogrep - " + baseDir + " - " + extensions.mkString(" ") + " - " + pattern)

    watcher.foreach(_.monitor.stop())
    val searcher = new FileSearcher(baseDir, pattern, updateRows)
    watcher = Some(new FileWatcher(baseDir, extensions, 5000, searcher))

  }


  def updateRows(matches: List[Match]): Unit = {
    model.setRowCount(0)
    matches.foreach(m => {
      val dir = FilenameUtils.getFullPath(m.filename)
      val filename = FilenameUtils.getName(m.filename)
      model.addRow(Array(dir, filename, m.lineNumber, m.line.trim).asInstanceOf[Array[Object]])
    })
    model.fireTableDataChanged()
    table.repaint()
  }

}
