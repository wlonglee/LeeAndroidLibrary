package com.lee.android.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成dimens
 */
public class GenerateDimenTool {
    public static void main(String[] args) {
        StringBuilder builder = new StringBuilder();
        //添加xml开始的标签
        String xmlStart = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n <resources xmlns:tools=\"http://schemas.android.com/tools\">\n";
        builder.append(xmlStart);
        for (int i = 1; i <= 960; i++) {
            String dimenName = "    <dimen name=\"px" + i + "\" tools:ignore=\"MissingDefaultResource\">";
            String end = "px</dimen>";
            builder.append(dimenName).append(i).append(end).append("\n");
        }
//        //添加sp
//        builder.append("\n\n\n<!--sp-->\n");
//        for (int i = 1; i <= 50; i++) {
//            String dimenName = "    <dimen name=\"sp" + i + "\" tools:ignore=\"MissingDefaultResource\">";
//            String end = "sp</dimen>";
//            builder.append(dimenName).append(i*1.5).append(end).append("\n");
//        }
        //添加xml的尾标签
        builder.append("</resources>");
        String dimensFile = "./app/src/main/res/values/dimens.xml";
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(dimensFile)));
            out.println(builder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.close();
    }
}

