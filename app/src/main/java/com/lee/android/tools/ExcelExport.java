package com.lee.android.tools;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * 导出多语言数据到xls
 *
 * @author lee
 * @date 2021/10/25
 */
public class ExcelExport {
    public static void main(String[] args) {
        //Ios使用 导出多语言txt数据到xls
//        exportTxtIos();

        //android使用 导出多语言xml数据到xls
        exportXlsAndroid();
    }

    private static final LinkedHashMap<String, ExcelData> DATA = new LinkedHashMap<>();

    //导出多语言到xls时的多语言文件路径-ios
    private static final String exportPathIos = "./app/src/main/assets/ios/";

    //导出多语言到xls时的多语言文件路径-android
    private static final String exportPathAndroid = "./app/src/main/res/";

    private static final String parseTabKeyName = "代码标记";
    private static final String parseSimplifiedKeyName = "简体";
    private static final String parseTraditionalTWKeyName = "繁体（台湾）";
    private static final String parseTraditionalHKKeyName = "繁体（香港）";
    private static final String parseEnKeyName = "英文";


    /**
     * 将values下的多语言strings.xml导出到xls中
     */
    private static void exportXlsAndroid() {
        String name = "导出数据";
        try {
            //导出生成的xls
            String excelFile = "./app/src/main/assets/导出数据.xls";

            //简体中文
            String resSimplified = exportPathAndroid + "values/strings.xml";
            //繁体中文
            String resTraditional = exportPathAndroid + "values-zh-rTW/strings.xml";
            //英语
            String resEn = exportPathAndroid + "values-en/strings.xml";

            FileOutputStream fos = new FileOutputStream(excelFile);
            WritableWorkbook workbook = Workbook.createWorkbook(fos);
            WritableSheet sheet = workbook.createSheet(name, 0);
            sheet.addCell(new Label(0, 0, parseTabKeyName));
            sheet.addCell(new Label(1, 0, parseSimplifiedKeyName));
            sheet.addCell(new Label(2, 0, parseTraditionalTWKeyName));
            sheet.addCell(new Label(3, 0, parseEnKeyName));

            //解析xml
            parseXml(resSimplified, 0);
            parseXml(resTraditional, 1);
            parseXml(resEn, 2);


            Iterator<Map.Entry<String, ExcelData>> iterator = DATA.entrySet().iterator();
            int c = 1;
            while (iterator.hasNext()) {
                Map.Entry<String, ExcelData> entry = iterator.next();
                String key = entry.getKey();
                ExcelData excelData = entry.getValue();
                //写入数据
                sheet.addCell(new Label(0, c, key));
                sheet.addCell(new Label(1, c, excelData.simplified));
                sheet.addCell(new Label(2, c, excelData.traditionalTW));
                sheet.addCell(new Label(3, c, excelData.en));
                c++;
            }
            workbook.write();
            workbook.close();
            fos.close();

            Log("xls导出完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String currentKey = "";

    private static void parseXml(String path, int type) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();


        parser.parse(path, new DefaultHandler() {
            @Override
            public void startDocument() throws SAXException {
                super.startDocument();
            }

            @Override
            public void endDocument() throws SAXException {
                super.endDocument();
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);
                if ("string".equals(qName)) {
                    String key = attributes.getValue(0);
                    currentKey = key;
                    if (!DATA.containsKey(key)) {
                        DATA.put(key, new ExcelData());
                    }
                }


            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length);
                String value = new String(ch, start, length).trim();
                if (!"".equals(value)) {
                    ExcelData data = DATA.get(currentKey);
                    if (data != null) {
                        switch (type) {
                            case 0:
                                data.simplified = value;
                                break;
                            case 1:
                                data.traditionalTW = value;
                                break;
                            case 2:
                                data.en = value;
                                break;
                        }
                        DATA.put(currentKey, data);
                    }
                }
            }
        });
    }

    /**
     * 将assets下的多语言txt导出到xls中
     */
    private static void exportTxtIos() {
        String name = "导出数据";
        try {
            //导出生成的xls
            String excelFile = "./app/src/main/assets/导出数据.xls";

            //简体中文
            String resSimplified = exportPathIos + "cn.txt";
            //英语
            String resEn = exportPathIos + "en.txt";

            FileOutputStream fos = new FileOutputStream(excelFile);
            WritableWorkbook workbook = Workbook.createWorkbook(fos);
            WritableSheet sheet = workbook.createSheet(name, 0);
            sheet.addCell(new Label(0, 0, parseTabKeyName));
            sheet.addCell(new Label(1, 0, parseSimplifiedKeyName));
            sheet.addCell(new Label(2, 0, parseTraditionalTWKeyName));
            sheet.addCell(new Label(3, 0, parseEnKeyName));

            //解析
            parseTxt(resSimplified, 0);
            parseTxt(resEn, 2);


            Iterator<Map.Entry<String, ExcelData>> iterator = DATA.entrySet().iterator();
            int c = 1;
            while (iterator.hasNext()) {
                Map.Entry<String, ExcelData> entry = iterator.next();
                String key = entry.getKey();
                ExcelData excelData = entry.getValue();
                //写入数据
                sheet.addCell(new Label(0, c, key));
                sheet.addCell(new Label(1, c, excelData.simplified));
                sheet.addCell(new Label(2, c, excelData.traditionalTW));
                sheet.addCell(new Label(3, c, excelData.en));
                c++;
            }
            workbook.write();
            workbook.close();
            fos.close();

            Log("xls导出完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param path 路径
     * @param type 0简中 1繁中  2 英文
     */
    private static void parseTxt(String path, int type) throws Exception {

        BufferedReader fis = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));

        String str = fis.readLine();
        String[] strList;

        while (str != null) {
            if (str.startsWith("\"")) {
                Log(str);
                strList = str.split("=");
                String key = strList[0].split("\"")[1];

                Log("key", key);

                String value = strList[1].split("\"")[1];
                Log("value", value);
                if (!DATA.containsKey(key)) {
                    DATA.put(key, new ExcelData());
                }
                ExcelData data = DATA.get(key);
                if(data!=null){
                    switch (type) {
                        case 0: {
                            data.simplified = value;
                        }
                        case 1: {

                        }
                        case 2: {
                            data.en = value;
                        }
                    }

                    DATA.put(key, data);
                }

            }
            str = fis.readLine();
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
    }

    public static void Log(Object obj) {
        System.out.println(obj.toString());
    }

    public static void Log(String tag, Object obj) {
        System.out.println(tag + ":" + obj.toString());
    }
}
