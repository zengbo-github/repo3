package com.example.readfile;


import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.File;
import java.io.IOException;

public class ImportExcel {

    public static void main(String[] args) throws IOException, BiffException {

       //Workbook book=Workbook.getWorkbook();
        Workbook book=Workbook.getWorkbook(new File("C:\\工作\\文件\\dome.xls"));
        Sheet sheet=book.getSheet(0);
        StringBuffer sql=new StringBuffer();
        			     //行34  从第4到第34   //小月33  大月34
            Cell cell1=sheet.getCell(0,1);

            System.out.println("cell1:"+cell1.getContents());


    }
}
