package main;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import okhttp3.*;
import org.apache.commons.dbutils.QueryRunner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import task.AnalysisTask;
import task.CheckDetailedListCrawlTask;
import task.TrafficInternetRecordsCrawlTask;
import utils.JdbcUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author sl
 */
public class Application {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(30);

    private static final OkHttpClient OK_HTTP_CLIENT;

    private static final HashMap<String, List<Cookie>> cookieStore = new HashMap<String, List<Cookie>>();

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(new CookieJar() {


            public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                cookieStore.put(httpUrl.host(), list);
            }

            public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                List<Cookie> cookies = cookieStore.get("login.189.cn");
                return cookies != null ? cookies : new ArrayList<Cookie>();

            }
        });
        OK_HTTP_CLIENT = builder.build();
    }


    private static final Retryer<Void> retryer = RetryerBuilder.<Void>newBuilder()
            .retryIfExceptionOfType(Exception.class)
            .retryIfRuntimeException()
            .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();


    public static void main(final String[] args) {


        try {

            //示例 1
            final String startTime = System.getProperty("startTime");

            //示例 30
            final String endTime = System.getProperty("endTime");

            //示例 2018年01月
            final String qryMonth = System.getProperty("qryMonth");

            //主表cookie
            final String cookie = System.getProperty("cookie");

            //从表cookie
            final String trafficInternetRecordsCookie = "code_v=20170913; dqmhIpCityInfos=%E5%8C%97%E4%BA%AC%E5%B8%82+%E8%81%94%E9%80%9A; svid=9CFB282DD73A0D0B; s_fid=249715028C12EE4E-32BFDBF204DA72E7; lvid=071d8dbcec067f385b958257db0b5d94; nvid=1; trkId=9A3FF427-79E7-4613-A61E-8AFB05EC6063; WT_FPC=id=19cfd108320580cd6e41515405264911; d_source=other; i_vnum=1; ijg=1515479749275; cityCode=bj; s_cc=true; SHOPID_COOKIEID=10001; trkHmPageName=%2Fbj%2F; trkHmCoords=0; trkHmCity=0; trkHmLinks=0; userId=201%7C20170100000012639029; .ybtj.189.cn=EC7C51BA55C003625227EF5B11FF1761; JSESSIONID_bj=L5JshVrTLyZ2Ld2TydcmDn8LbWxRJQLn07LJKRf46sd7TpNw7RT2!-1903192657; WT_SS=1515547574536c057ed536; WT_si_n=WEB_Q_MYLIST_THEPHONELIST_XDCX; loginStatus=non-logined; Hm_lvt_5b3beae528c7fc9af9c016650f4581e0=1515498788,1515498829,1515547574,1515549229; Hm_lpvt_5b3beae528c7fc9af9c016650f4581e0=1515549229; s_sq=eshipeship-189-all%3D%2526pid%253D%25252Fiframe%25252Flocal%25252FflowInit.action%2526pidt%253D1%2526oid%253Djavascript%25253Apage%25252854%25252C2%252529%2526ot%253DA; trkHmClickCoords=951%2C440%2C4779";

            //流量过滤阈值 单位：M
            final Integer dataFlowThresholdValue = Integer.valueOf(System.getProperty("dataFlowThresholdValue"));
            retryer.call(new Callable<Void>() {
                public Void call() throws Exception {

                    if ("1".equals(args[0])) {
                        //用于产生 费用查询->通信详单查询中的上网详单   记录少的A表  主表
                        executorService.submit(new CheckDetailedListCrawlTask(startTime, endTime, qryMonth, cookie, dataFlowThresholdValue));

                    }

                    return null;
                }

            });



            retryer.call(new Callable<Void>() {
                public Void call() throws Exception {

                    if ("1".equals(args[1])) {
                        //用于产生 套餐查询->流量使用去向查询->流量上网记录  记录多的B表 从表
                        crawlTrafficInternetRecordsByMultithreadedThread(Integer.valueOf(startTime), Integer.valueOf(endTime), qryMonth, trafficInternetRecordsCookie);

                    }

                    return null;
                }

            });

            while (true) {
                Thread.sleep(2000);
                if (((ThreadPoolExecutor) executorService).getActiveCount() == 0) {
                    break;
                }
            }

            AnalysisTask.analysis();
            System.exit(1);

            executorService.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public static boolean crawlTrafficInternetRecordsByMultithreadedThread(Integer start, Integer end, String qryMonth, String cookie) {

        try {
            String sql = "delete from traffic_internet_records  where date_format(start_time, '%Y-%m-%d') = ?";
            QueryRunner runner = new QueryRunner(JdbcUtil.getDataSource());

            for (int i = start; i < end + 1; i++) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, Integer.parseInt(qryMonth.substring(0, 4)));
                calendar.set(Calendar.MONTH, Integer.parseInt(qryMonth.substring(5, 7)) - 1);
                calendar.set(Calendar.DAY_OF_MONTH, i);
                runner.update(sql, new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));

                TrafficInternetRecordsCrawlTask task = new TrafficInternetRecordsCrawlTask(String.valueOf(i), qryMonth, cookie);
                executorService.submit(task);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        return false;
    }

    /**
     * 开发中
     *
     * @return
     * @throws Exception
     */
    public static String getCookie() throws Exception {


        try {

            String codeUrl = "http://login.189.cn/web/captcha?undefined&source=login&width=100&height=37&0.2707323502871577";


            Request request = new Request.Builder()
                    .url(codeUrl)
                    .addHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                    .addHeader("Cache-Control", "no-cache")
                    .get()
                    .build();

            Response response = OK_HTTP_CLIENT.newCall(request).execute();

            File imageFile = new File(System.getProperty("user.dir") + File.separator + "doc" + File.separator + "code.png");

            FileOutputStream output = new FileOutputStream(imageFile);
            //得到网络资源的字节数组,并写入文件
            output.write(response.body().bytes());
            output.close();

            BufferedReader strin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("请输入验证码：");
            String captcha = strin.readLine();


            String url = "http://login.189.cn/web/login";


            RequestBody formBody = new FormBody.Builder()
                    .add("Account", "15313750907")
                    .add("UType", "3")
                    .add("ProvinceID", "01")
                    .add("RandomFlag", "0")
                    .add("Password", "aEbkoxW7piPx9abYYNDl8A==")
                    .add("Captcha", captcha)
                    .build();


            Request loginRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Content-type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Referer", "http://login.189.cn/web/login")
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .post(formBody)
                    .build();

            Response loginResponse = OK_HTTP_CLIENT.newCall(loginRequest).execute();
            Document doc = Jsoup.parse(loginResponse.body().string());

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
