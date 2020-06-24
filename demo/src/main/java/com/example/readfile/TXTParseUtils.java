package com.example.readfile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TXTParseUtils {

    private static final Integer ONE = 1;

    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        StringBuffer sql=new StringBuffer();

        /* 读取数据 */
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("C:\\工作\\文件\\fnr_area.dat")), "GBK"));
            String lineTxt = null;
            while ((lineTxt = br.readLine()) != null) {
                System.out.println(lineTxt);
                System.out.println("=============================");
                //String[] names = lineTxt.split("\u001C");
                String[] names = lineTxt.split("\u001C");
                sql.append("insert into report (xxx,xxx,xxx,xxx,xxx,xxx) values ('");
                for (String name : names) {
                    sql.append(name);
                    sql.append("','");

                    /*if (map.keySet().contains(name)) {
                        map.put(name, (map.get(name) + ONE));
                    } else {
                        map.put(name, ONE);
                    }*/
                }
                sql.append("');");
                String sqlString=sql.toString();
                wirterTxt(sqlString);
                int  sb_length = sql.length();// 取得字符串的长度
                sql.delete(0,sb_length);    //删除字符串从0~sb_length-1处的内容 (这个方法就是用来清除StringBuffer中的内容的)

            }
            br.close();
        } catch (Exception e) {
            System.err.println("read errors :" + e);
        }


      /*  *//* 输出数据 *//*
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("C:\\工作\\文件\\结果文件.txt")), "UTF-8"));

            for (String name : map.keySet()) {
                String sql="insert into tablename values("+"'"+name+"')";
                System.out.println("sql:"+sql);
                //bw.write(sql + " " + map.get(name));
                bw.write(name + " " + map.get(name));
                bw.newLine();

            }
            System.out.println("*********************");
            bw.close();
        } catch (Exception e) {
            System.err.println("write errors :" + e);
        }*/
    }

    public static void wirterTxt(String sqlString) throws IOException {
        String fileName = "C:/工作/文件/结果文件.txt";

        FileWriter writer = new FileWriter(fileName, true);

        writer.write(sqlString);
        writer.write("\r\n");
        writer.close();

        /* 输出数据 */
        /*try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("C:\\工作\\文件\\结果文件.txt")), "UTF-8"));

            bw.write(sqlString);
            bw.newLine();
          *//*  for (String  name : map.keySet()) {
                String sql="insert into tablename values("+"'"+name+"')";
                System.out.println("sql:"+sql);
                //bw.write(sql + " " + map.get(name));
                bw.write(name + " " + map.get(name));
                bw.newLine();

            }*//*
            System.out.println("*********************");
            bw.close();
        } catch (Exception e) {
            System.err.println("write errors :" + e);
        }*/
    }
}

