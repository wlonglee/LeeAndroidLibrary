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
 * 针对ios解析
 */
public class ExcelToolIos {

    public static void main(String[] args) {
        //解析xls生成多语言txt
        parseXsl(parsePath);
    }

    private static final LinkedHashMap<String, ExcelData> DATA = new LinkedHashMap<>();
    //解析的文件
    private static final String parsePath = "./app/src/main/assets/online.xlsx";
    //解析的表格名
    private static final String parseSheetName = "iOS";
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
    //解析后生成的多语言文件路径
    private static final String path = "./app/src/main/assets/multilingual/";


    private static void parseXsl(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            Workbook wb = null;
            if (path.endsWith(".xls")) {
                wb = new HSSFWorkbook(fis);
            } else if (path.endsWith(".xlsx")) {
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
            //生成 多语言文件
            generateTxt("中文", 0);
            generateTxt("繁体-台湾", 1);
            generateTxt("英文", 2);
            generateTxt("繁体-香港", 3);
            generateTxt("德语", 4);
            generateTxt("法语", 5);
            generateTxt("日语", 6);
            generateTxt("西语", 7);
            Log("数据导入完成");
        } catch (Exception e) {
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
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC: {
                return String.valueOf((int) Math.floor(cell.getNumericCellValue())).trim();
            }
            case Cell.CELL_TYPE_STRING: {
                return cell.getStringCellValue().trim();
            }
            default:
                return "";
        }
    }

    private static void generateTxt(String name, int type) {
        File file = new File(path);
        if (!file.exists()) {
            boolean ok = file.mkdirs();
            if (!ok) {
                Log("创建文件夹失败");
            }
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ExcelData> entry : DATA.entrySet()) {
            String key = entry.getKey();
            ExcelData excelData = entry.getValue();
            String start = "\"" + key + "\" = ";
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
            String end = ";";
            builder.append(start).append("\"").append(m).append("\"").append(end).append("\n");
        }
        String xmlFile = path + name + ".txt";
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(xmlFile)))) {
            out.println(builder);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static class ExcelData {
        //代码标记
        public String key = "";
        //简体
        public String simplified = "";
        //繁体台湾
        public String traditionalTW = "";
        //繁体香港
        public String traditionalHK = "";
        //英语
        public String en = "";
        //德语
        public String german = "";
        //法语
        public String french = "";
        //日语
        public String japanese = "";
        //西班牙语
        public String spanish = "";

        @Override
        public String toString() {
            return "{" +
                    "代码标记='" + key + '\'' +
                    ", 简体='" + simplified + '\'' +
                    ", 繁体（台湾）='" + traditionalTW + '\'' +
                    ", 繁体（香港）='" + traditionalHK + '\'' +
                    ", 英文='" + en + '\'' +
                    ", 德语='" + german + '\'' +
                    ", 法语='" + french + '\'' +
                    ", 日语='" + japanese + '\'' +
                    ", 西语='" + spanish + '\'' +
                    '}';
        }

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

    public static void Log(String tag, Object obj) {
        System.out.println(tag + ":" + obj.toString());
    }

    public static void Log(Object obj) {
        System.out.println(obj.toString());
    }
}

