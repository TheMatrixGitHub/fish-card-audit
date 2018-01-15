package task;

import com.alibaba.fastjson.JSONObject;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import entity.DetailedFlow;
import okhttp3.*;
import org.apache.commons.dbutils.QueryRunner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.ForMatJSONStr;
import utils.JdbcUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author sl
 */
public class CheckDetailedListCrawlTask implements Callable {


    private String startTime;
    private String endTime;
    private String qryMonth;
    private String cookie;

    //流量过滤阈值
    private Integer dataFlowThresholdValue;

    public CheckDetailedListCrawlTask(String startTime, String endTime, String qryMonth, String cookie, Integer dataFlowThresholdValue) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.qryMonth = qryMonth;
        this.cookie = cookie;
        this.dataFlowThresholdValue = dataFlowThresholdValue;
    }

    private static final OkHttpClient client = new OkHttpClient();


    public Object call() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String accNum = "15313750907";

        //通信详单查询接口，该接口仅返回开始时间、时长、流量使用量以及是否为定向套餐
        String url = "http://bj.189.cn/iframe/feequery/billDetailQuery.action";

        try {
            QueryRunner runner = new QueryRunner(JdbcUtil.getDataSource());

            //首先清除detailed_flow表中的记录
            String truncateSql = "truncate table detailed_flow";
            runner.update(truncateSql);

            String insertSql = "INSERT INTO detailed_flow  (" +
                    "start_time," +
                    "end_time," +
                    "flow," +
                    "type," +
                    "belong_area," +
                    "key_type) VALUES (?,?,?,?,?,?)";


            RequestBody formBody = new FormBody.Builder()
                    .add("requestFlag", "synchronization")
                    .add("billDetailType", "3")
                    .add("qryMonth", qryMonth)
                    .add("accNum", accNum)
                    .add("startTime", startTime)
                    .add("endTime", endTime)
                    .build();


            //已经处理的问题详单条数
            int realCount = 0;

            //已经处理的实际大于阈值订单条数
            int overThresholdCount = 0;

            loop:
            for (int i = 1; i < Integer.MAX_VALUE; i++) {


                if (i != 1) {
                    formBody = new FormBody.Builder()
                            .add("requestFlag", "synchronization")
                            .add("billDetailType", "3")
                            .add("qryMonth", qryMonth)
                            .add("accNum", accNum)
                            .add("startTime", startTime)
                            .add("endTime", endTime)
                            .add("billPage", String.valueOf(i))
                            .build();
                }

                Request request = new Request.Builder()
                        .url(url)
                        .header("cookie", cookie)
                        .addHeader("Connection", "keep-alive")
                        .addHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .addHeader("Accept", "text/html, */*; q=0.01")
                        .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                        .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                        .addHeader("Cache-Control", "no-cache")
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .post(formBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }


                Document doc = Jsoup.parse(response.body().string());


                Elements e = doc.getElementsByClass("color-6 fs-16");
                int total = Integer.parseInt(e.get(0).text());

                Elements elements = doc.select("tbody").select("tr");

                for (Element element : elements) {
                    Elements billDetailElements = element.select("td");
                    if (billDetailElements.size() == 8) {

                        if (realCount == total) {
                            break loop;
                        } else {
                            realCount++;
                        }

                        String dataFlowValue = billDetailElements.get(3).text();
                        int index;


                        if ((index = dataFlowValue.indexOf("MB")) != -1 && Integer.parseInt(dataFlowValue.substring(0, index)) > dataFlowThresholdValue && billDetailElements.get(7).text().contains("非定向")) {
                            DetailedFlow detailedFlow = new DetailedFlow();

                            Date startDate = dateFormat.parse(billDetailElements.get(1).text());

                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(startDate);

                            String durationTime = billDetailElements.get(2).text();
                            int minuteIndex = durationTime.indexOf("分钟");
                            int secondIndex = durationTime.indexOf("秒");
                            if (minuteIndex != -1) {
                                calendar.add(Calendar.MINUTE, Integer.parseInt(durationTime.substring(0, minuteIndex)));
                            }

                            if (secondIndex != -1) {
                                if (minuteIndex != -1) {
                                    calendar.add(Calendar.SECOND, Integer.parseInt(durationTime.substring(minuteIndex + 2, secondIndex)));
                                } else {
                                    calendar.add(Calendar.SECOND, Integer.parseInt(durationTime.substring(0, secondIndex)));
                                }

                            }

                            detailedFlow.setStart_time(startDate);
                            detailedFlow.setEnd_time(calendar.getTime());
                            detailedFlow.setFlow(billDetailElements.get(3).text());
                            detailedFlow.setType(billDetailElements.get(4).text());
                            detailedFlow.setBelong_area(billDetailElements.get(5).text());
                            detailedFlow.setKey_type(billDetailElements.get(7).text());

                            runner.update(insertSql,
                                    detailedFlow.getStart_time(),
                                    detailedFlow.getEnd_time(),
                                    detailedFlow.getFlow(),
                                    detailedFlow.getType(),
                                    detailedFlow.getBelong_area(),
                                    detailedFlow.getKey_type());

                            overThresholdCount++;
                            System.out.printf("当前已处理条数：%s", overThresholdCount);
                            System.out.println(ForMatJSONStr.format(JSONObject.toJSONString(detailedFlow)));

                        }
                    }
                }
            }


            System.out.println("问题详单任务处理完毕！");
        } catch (
                Exception e)

        {
            e.printStackTrace();
            throw e;
        }
        return null;

    }
}
