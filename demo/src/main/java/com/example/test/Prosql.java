package com.example.test;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.log4j.Logger;


import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Prosql {

    public static Logger logger =Logger.getLogger(Prosql.class);

    private  static  int colflag;


    public static Map<String,String> readExcelProperties() throws IOException, BiffException {
        Map<String,String>  map=new HashMap<>();
        Workbook book=Workbook.getWorkbook(new File("C:\\工作\\文件\\dome.xls"));
        Sheet sheet=book.getSheet(0);
        Cell toTable=sheet.getCell(0,1);
        map.put("toTable",toTable.getContents());
        Cell readFile=sheet.getCell(1,1);
        map.put("readFile",readFile.getContents());
        Cell colNum=sheet.getCell(0,3);
        map.put("colNum",colNum.getContents());
        logger.info("colNum:"+colNum.getContents());
        //转换列数为int
        System.out.println("colNum.getContents():"+colNum.getContents());
        int j=Integer.valueOf(colNum.getContents());
        colflag=j+5;
        for(int i=5;i<colflag;i++){
            String s="col"+i;
            String v="val"+i;
            Cell a=sheet.getCell(0,i);
            Cell b=sheet.getCell(1,i);
            map.put(s,a.getContents());
            map.put(v,b.getContents());
            logger.info("a:"+a.getContents());
        }
        logger.info("map:"+map);
        return map;
   }

    public static void readTxtFile(Map<String, String> map) throws IOException {
        StringBuffer sqlPrefix=new StringBuffer();
        String readFile=map.get("readFile");
        String toTable=map.get("toTable");
        sqlPrefix.append("insert into ");
        sqlPrefix.append(toTable);
        sqlPrefix.append(" (");
        for(int i=5;i<colflag;i++){
            String s="col"+i;
            String v="val"+i;
            String col=map.get(s);
            if(i==(colflag-1)){
                sqlPrefix.append(col);
                sqlPrefix.append(") values (");
            }else{
                sqlPrefix.append(col);
                sqlPrefix.append(",");
            }
        }
        logger.info("sql:"+sqlPrefix.toString());

        //StringBuffer sql=new StringBuffer(sqlPrefix.toString());

        String pathname="C:\\工作\\文件\\"+readFile;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
                new File(pathname)), "GBK"));
        String lineTxt = null;
        while ((lineTxt = br.readLine()) != null) {
            StringBuffer sql=new StringBuffer(sqlPrefix.toString());
            System.out.println("原始一行数据"+lineTxt);
            System.out.println("=============================");
            String[] names = lineTxt.split("\u001C");
            for(int i=5;i<colflag;i++){
                String v="val"+i;
                String val=map.get(v);
                logger.info("val:"+val);
                if(i==(colflag-1)){
                    if(val!=""){
                        int valnum=Integer.valueOf(map.get(v));
                        sql.append("'");
                        sql.append(names[valnum]);
                        sql.append("');");
                    }else {
                        sql.append("' '");
                        sql.append(",");
                    }
                }else{
                    if(val!=""){
                        int valnum=Integer.valueOf(map.get(v));
                        sql.append("'");
                        sql.append(names[valnum]);
                        sql.append("',");
                    }else {
                        sql.append("' '");
                        sql.append(",");
                    }
                }
            }
            String sqlString=sql.toString();
            wirterTxt(sqlString);
            int  sb_length = sql.length();// 取得字符串的长度
            sql.delete(0,sb_length);    //删除字符串从0~sb_length-1处的内容 (这个方法就是用来清除StringBuffer中的内容的)

        }

    }

    public static void wirterTxt(String sqlString) throws IOException {
        String fileName = "C:/工作/文件/结果文件.txt";

        FileWriter writer = new FileWriter(fileName, true);

        writer.write(sqlString);
        writer.write("\r\n");
        writer.close();
    }


    public static void main(String[] args) throws IOException, BiffException {
        readTxtFile(readExcelProperties());

    }
}
