package com.lee.android.tools;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析Excel
 */
public class ExcelTool {

    public static void main(String[] args) {
        //解析生成多语言xml
        parseXls(parsePath);
    }


    private static final LinkedHashMap<String, ExcelData> DATA = new LinkedHashMap<>();

    //解析的文档地址
    private static final String parsePath = "./app/src/main/assets/online.xlsx";

    //解析的表格名
    private static final String parseSheetName = "循环乐段";
    //解析的字段名称
    private static final String parseTabKeyName = "代码标记";
    private static final String parseSimplifiedKeyName = "简体";
    private static final String parseTraditionalTWKeyName = "繁体（台湾）";
    private static final String parseTraditionalHKKeyName = "繁体（香港）";
    private static final String parseEnKeyName = "英文";
    private static final String parseGermanKeyName = "德语";
    private static final String parseFrenchKeyName = "法语";
    private static final String parseJapaneseKeyName = "日语";
    private static final String parseSpanishKeyName = "西语";
    //解析xsl后生成的多语言文件路径
    private static final String path = "./app/src/main/assets/multilingual/";

    private static void parseXls(String parsePath) {
        try {
            FileInputStream fis = new FileInputStream(parsePath);
            Workbook wb = null;
            if (parsePath.endsWith(".xls")) {
                wb = new HSSFWorkbook(fis);
            } else if (parsePath.endsWith(".xlsx")) {
                wb = new XSSFWorkbook(fis);
            }

            if (wb == null) {
                Log("不支持的文件");
                return;
            } else {
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    Sheet sheet = wb.getSheetAt(i);
                    if (parseSheetName.equals(sheet.getSheetName())) {
                        parseSheet(sheet,
                                parseTabKeyName,
                                parseSimplifiedKeyName,
                                parseTraditionalTWKeyName,
                                parseTraditionalHKKeyName,
                                parseEnKeyName,
                                parseGermanKeyName,
                                parseFrenchKeyName,
                                parseJapaneseKeyName,
                                parseSpanishKeyName
                        );
                    }
                }
            }
            wb.close();
            fis.close();
            //生成 多语言xml文件
            generateXML(path + "values-zh-rCN", 0);
            generateXML(path + "values-zh-rTW", 1);
            generateXML(path + "values-en", 2);
            generateXML(path + "values-zh-rHK", 3);
            generateXML(path + "values-de-rDE", 4);
            generateXML(path + "values-fr-rFR", 5);
            generateXML(path + "values-ja-rJP", 6);
            generateXML(path + "values-es-rES", 7);
            Log("数据导入完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateXML(String xmlPath, int type) {
        File file = new File(xmlPath);
        if (!file.exists()) {
            boolean ok = file.mkdirs();
            if (!ok) {
                Log("创建文件夹失败");
            }
        }

        StringBuilder builder = new StringBuilder();
        String xmlStart = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n";
        builder.append(xmlStart);
        for (Map.Entry<String, ExcelData> entry : DATA.entrySet()) {
            String key = entry.getKey();
            ExcelData excelData = entry.getValue();
            String xmlData = "    <string name=\"" + key + "\">";
            String m = "";
            switch (type) {
                case 0:
                    m = excelData.simplified;
                    break;
                case 1:
                    m = excelData.traditionalTW;
                    break;
                case 2:
                    m = excelData.en;
                    break;
                case 3:
                    m = excelData.traditionalHK;
                    break;
                case 4:
                    m = excelData.german;
                    break;
                case 5:
                    m = excelData.french;
                    break;
                case 6:
                    m = excelData.japanese;
                    break;
                case 7:
                    m = excelData.spanish;
                    break;

            }
            String end = "</string>";
            builder.append(xmlData).append(m).append(end).append("\n");
        }
        builder.append("</resources>");

        String xmlFile = xmlPath + "/strings.xml";
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(xmlFile)))) {
            out.println(builder);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void parseSheet(Sheet sheet, String... parseName) {
        //获取最大行数
        int rowNum = sheet.getPhysicalNumberOfRows();
        //获取首行数据
        Row rowHeader = sheet.getRow(0);
        int colNum = rowHeader.getPhysicalNumberOfCells();


        //开始解析数据
        Row row;
        for (int i = 1; i < rowNum; i++) {
            row = sheet.getRow(i);
            if (row != null) {
                ExcelData data = new ExcelData();
                for (int j = 0; j < colNum; j++) {
                    int index = contains(rowHeader.getCell(j), parseName);
                    switch (index) {
                        case 0:
                            data.key = getCellValue(row.getCell(j));
                            break;
                        case 1:
                            data.simplified = getCellValue(row.getCell(j));
                            break;
                        case 2:
                            data.traditionalTW = getCellValue(row.getCell(j));
                            break;
                        case 3:
                            data.traditionalHK = getCellValue(row.getCell(j));
                            break;
                        case 4:
                            data.en = getCellValue(row.getCell(j));
                            break;
                        case 5:
                            data.german = getCellValue(row.getCell(j));
                            break;
                        case 6:
                            data.french = getCellValue(row.getCell(j));
                            break;
                        case 7:
                            data.japanese = getCellValue(row.getCell(j));
                            break;
                        case 8:
                            data.spanish = getCellValue(row.getCell(j));
                            break;
                    }
                }
                if (!data.key.equals("")) {
                    DATA.put(data.key, data);
                    if (data.isEmpty()) {
                        Log((i+1)+"行存在空数据", data);
                    }
                }
            }
        }

    }

    /**
     * 指定列表格是否是需要解析的表格
     */
    private static int contains(Cell cell, String... parseName) {
        String cellVal = getCellValue(cell);
        for (int i = 0; i < parseName.length; i++) {
            if (cellVal.equals(parseName[i]))
                return i;
        }
        return -1;
    }

    private static String getCellValue(Cell cell) {
        String s = "";
        if (cell == null)
            return s;
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC: {
                s = String.valueOf((int) Math.floor(cell.getNumericCellValue())).trim();
                break;
            }
            case Cell.CELL_TYPE_STRING: {
                s = cell.getStringCellValue().trim();
                break;
            }
            default:
                s = "";
                break;
        }
        if (s.contains("\"")) {
            s= s.replaceAll("\"", "\\\\\"");
        }

        if (s.contains("'")) {
            s= s.replaceAll("'", "\\\\'");
        }

        return s;
    }


    public static class ExcelData {
        //代码标记-0
        public String key = "";
        //简体-1
        public String simplified = "";
        //繁体台湾-2
        public String traditionalTW = "";
        //繁体香港-3
        public String traditionalHK = "";
        //英语-4
        public String en = "";
        //德语-5
        public String german = "";
        //法语-6
        public String french = "";
        //日语-7
        public String japanese = "";
        //西班牙语-8
        public String spanish = "";

        public boolean isEmpty() {
            if ("".equals(simplified))
                return true;
            if ("".equals(traditionalTW))
                return true;
            if ("".equals(traditionalHK))
                return true;
            if ("".equals(en))
                return true;
            if ("".equals(german))
                return true;
            if ("".equals(french))
                return true;
            if ("".equals(japanese))
                return true;
            return "".equals(spanish);
        }
    }


    public static void Log(Object obj) {
        System.out.println(obj.toString());
    }

    public static void Log(String tag, Object obj) {
        System.out.println(tag + ":" + obj.toString());
    }
}

