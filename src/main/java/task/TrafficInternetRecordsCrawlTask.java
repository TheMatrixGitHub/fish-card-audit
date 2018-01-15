package task;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import entity.TrafficInternetRecords;
import okhttp3.*;
import org.apache.commons.dbutils.QueryRunner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.JdbcUtil;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author sl
 */
public class TrafficInternetRecordsCrawlTask implements Callable {

    private String qryTime;
    private String qryMonth;
    private String cookie;

    public TrafficInternetRecordsCrawlTask(String qryTime, String qryMonth, String cookie) {
        this.qryTime = qryTime;
        this.qryMonth = qryMonth;
        this.cookie = cookie;
    }

    private static final OkHttpClient client = new OkHttpClient();


    public Object call() throws Exception {


        String accNum = "15313750907";
        String flowPageSize = "50";

        String url = "http://bj.189.cn/iframe/local/queryFlowRecord.action";

        try {

            QueryRunner runner = new QueryRunner(JdbcUtil.getDataSource());

            String insertSql = "INSERT INTO traffic_internet_records  (" +
                    "start_time," +
                    "business_name," +
                    "url) VALUES (?,?,?)";


            int realCount = 0;

            loop:
            for (int i = 1; i < Integer.MAX_VALUE; i++) {


                RequestBody formBody = new FormBody.Builder()
                        .add("requestFlag", "synchronization")
                        .add("billDetailType", "3")
                        .add("qryMonth", qryMonth)
                        .add("accNum", accNum)
                        .add("qryTime", qryTime)
                        .add("flowPageSize", flowPageSize)
                        .add("flowPage", String.valueOf(i))
                        .build();


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


                String responseString = response.body().string();
                if (responseString.contains("未查询到您的详单信息")) {

                    break loop;
                }

                Document doc = Jsoup.parse(responseString);


                Elements elements = doc.select("tbody").select("tr");

                for (Element element : elements) {
                    Elements tdElements = element.select("td");
                    if (tdElements.size() == 4) {


                        TrafficInternetRecords trafficInternetRecord = new TrafficInternetRecords();
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, Integer.parseInt(qryMonth.substring(0, 4)));
                        calendar.set(Calendar.MONTH, Integer.parseInt(qryMonth.substring(5, 7)) - 1);
                        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(qryTime));
                        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tdElements.get(1).text().substring(0, 2)));
                        calendar.set(Calendar.MINUTE, Integer.parseInt(tdElements.get(1).text().substring(3, 5)));
                        calendar.set(Calendar.SECOND, Integer.parseInt(tdElements.get(1).text().substring(6, 8)));


                        trafficInternetRecord.setStart_time(calendar.getTime());
                        trafficInternetRecord.setBusiness_name(tdElements.get(2).text());
                        trafficInternetRecord.setUrl(tdElements.get(3).text());

                        runner.update(insertSql,
                                trafficInternetRecord.getStart_time(),
                                trafficInternetRecord.getBusiness_name(),
                                trafficInternetRecord.getUrl());
                        realCount++;

                        System.out.printf("%s 已处理条数: %s", Thread.currentThread().getName(), realCount);
                        System.out.println();

                    }
                }
            }

            System.out.println("任务执行完毕！");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return null;

    }
}
