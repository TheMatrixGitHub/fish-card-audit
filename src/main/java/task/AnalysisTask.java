package task;

import com.alibaba.fastjson.JSONObject;
import entity.DetailedFlow;
import entity.TrafficInternetRecords;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import utils.ForMatJSONStr;
import utils.JdbcUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sl
 */
public class AnalysisTask {

    private static final String filePath = System.getProperty("user.dir") + File.separator + "doc" + File.separator;

    public static void analysis() {
        try {

            // 创建工作薄
            XSSFWorkbook workBook = new XSSFWorkbook();
            // 在工作薄中创建一工作表
            XSSFSheet sheet = workBook.createSheet();


            QueryRunner runner = new QueryRunner(JdbcUtil.getDataSource());

            String sql = "select * from detailed_flow";

            List<DetailedFlow> detailedFlows = runner.query(sql, new BeanListHandler<DetailedFlow>(DetailedFlow.class));


            for (DetailedFlow detailedFlow : detailedFlows) {

                String trafficInternetRecordsSql = "SELECT * FROM traffic_internet_records t where t.start_time >= ? and t.start_time <= ?";

                List<TrafficInternetRecords> trafficInternetRecords = runner.query(trafficInternetRecordsSql, new BeanListHandler<TrafficInternetRecords>(TrafficInternetRecords.class), detailedFlow.getStart_time(), detailedFlow.getEnd_time());


                if (trafficInternetRecords.size() != 0) {
//                    System.out.println(ForMatJSONStr.format(JSONObject.toJSONString(detailedFlow)));
                    int lastRowNum = sheet.getLastRowNum() + 1;

                    for (int k = 0; k < trafficInternetRecords.size(); k++) {
                        // 在指定的索引处创建一行
                        XSSFRow row = sheet.createRow(k + lastRowNum);

                        // 在指定的索引处创建一列（单元格）
                        XSSFCell detailedFlowCell = row.createCell(0);
                        Map map = new HashMap(8);
                        map.put("开始时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(detailedFlow.getStart_time()));
                        map.put("结束时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(detailedFlow.getEnd_time()));
                        map.put("流量", detailedFlow.getFlow());
                        map.put("网络类型", detailedFlow.getType());
                        map.put("通信地点", detailedFlow.getBelong_area());
                        map.put("套餐类型", detailedFlow.getKey_type());

                        XSSFRichTextString detailedFlowContent = new XSSFRichTextString(ForMatJSONStr.format(JSONObject.toJSONString(map)));
                        detailedFlowCell.setCellValue(detailedFlowContent);

                        // 在指定的索引处创建一列（单元格）
                        XSSFCell startTime = row.createCell(1);
                        XSSFRichTextString startTimeContent = new XSSFRichTextString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(trafficInternetRecords.get(k).getStart_time()));
                        startTime.setCellValue(startTimeContent);

                        XSSFCell businessName = row.createCell(2);
                        XSSFRichTextString businessNameContent = new XSSFRichTextString(trafficInternetRecords.get(k).getBusiness_name());
                        businessName.setCellValue(businessNameContent);

                        XSSFCell url = row.createCell(3);
                        XSSFRichTextString urlContent = new XSSFRichTextString(trafficInternetRecords.get(k).getUrl());
                        url.setCellValue(urlContent);

                    }

                    if (lastRowNum != lastRowNum + trafficInternetRecords.size() - 1) {

                        sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum + trafficInternetRecords.size() - 1, 0, 0));

                    }

                }
            }

            // 新建一输出流并把相应的excel文件存盘
            FileOutputStream fos = new FileOutputStream(filePath + "鱼卡投诉专用.xlsx");
            workBook.write(fos);
            fos.flush();
            //操作结束，关闭流
            fos.close();
            System.out.println("文件生成");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
