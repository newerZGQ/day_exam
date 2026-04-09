package com.gorden.dayexam.parser

import android.util.Log
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.BufferedInputStream
import java.io.FileInputStream

object ExcelTextExtractor {
    private const val TAG = "ExcelTextExtractor"

    fun extractText(filePath: String): String {
        return extractQuestionBlocks(filePath).joinToString("\n\n")
    }

    fun extractQuestionBlocks(filePath: String): List<String> {
        val formatter = DataFormatter()
        val blocks = mutableListOf<String>()
        val file = File(filePath)
        openWorkbook(file).use { workbook ->
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetBlocks = extractQuestionBlocks(sheet, formatter)
                blocks.addAll(sheetBlocks)
                Log.d(TAG, "sheet=${sheet.sheetName} questionBlocks=${sheetBlocks.size}")
            }
        }
        Log.d(TAG, "extract complete blocks=${blocks.size}")
        return blocks
    }

    private fun extractQuestionBlocks(sheet: Sheet, formatter: DataFormatter): List<String> {
        val blocks = mutableListOf<String>()
        val groupedRanges = groupRowsByFirstColumn(sheet)
        for (range in groupedRanges) {
            val lines = mutableListOf<String>()
            for (rowIndex in range.first..range.last) {
                val row = sheet.getRow(rowIndex) ?: continue
                val cells = mutableListOf<String>()
                val lastCellNum = row.lastCellNum.toInt()
                if (lastCellNum <= 0) {
                    continue
                }
                for (cellIndex in 0 until lastCellNum) {
                    val cell = row.getCell(cellIndex) ?: continue
                    val value = formatter.formatCellValue(cell).trim()
                    if (value.isNotEmpty()) {
                        cells.add(value)
                    }
                }
                if (cells.isNotEmpty()) {
                    lines.add(cells.joinToString(" | "))
                }
            }
            if (lines.isNotEmpty()) {
                blocks.add(lines.joinToString("\n"))
            }
        }
        return blocks
    }

    private fun groupRowsByFirstColumn(sheet: Sheet): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val mergedStarts = mutableMapOf<Int, Int>()
        val coveredRows = mutableSetOf<Int>()
        for (regionIndex in 0 until sheet.numMergedRegions) {
            val region = sheet.getMergedRegion(regionIndex)
            if (region.containsColumn(0)) {
                val span = region.lastRow - region.firstRow + 1
                mergedStarts[region.firstRow] = maxOf(mergedStarts[region.firstRow] ?: 1, span)
                for (rowIndex in region.firstRow + 1..region.lastRow) {
                    coveredRows.add(rowIndex)
                }
            }
        }

        var rowIndex = sheet.firstRowNum
        while (rowIndex <= sheet.lastRowNum) {
            if (coveredRows.contains(rowIndex)) {
                rowIndex++
                continue
            }
            val span = mergedStarts[rowIndex] ?: 1
            val endRow = minOf(sheet.lastRowNum, rowIndex + span - 1)
            ranges.add(rowIndex..endRow)
            rowIndex = endRow + 1
        }
        return ranges
    }

    private fun CellRangeAddress.containsColumn(columnIndex: Int): Boolean {
        return firstColumn <= columnIndex && lastColumn >= columnIndex
    }

    private fun openWorkbook(file: File): Workbook {
        val extension = file.extension.lowercase()
        return try {
            when (extension) {
                "xlsx" -> {
                    BufferedInputStream(FileInputStream(file)).use { inputStream ->
                        XSSFWorkbook(inputStream)
                    }
                }
                "xls" -> {
                    BufferedInputStream(FileInputStream(file)).use { inputStream ->
                        HSSFWorkbook(inputStream)
                    }
                }
                else -> {
                    BufferedInputStream(FileInputStream(file)).use { inputStream ->
                        WorkbookFactory.create(inputStream)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "openWorkbook failed extension=$extension", e)
            throw e
        }
    }
}
