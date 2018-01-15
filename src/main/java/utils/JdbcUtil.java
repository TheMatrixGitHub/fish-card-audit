package utils;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;


/**
 * DBCP配置类
 *
 * @author SUN
 */
public class JdbcUtil {

    private static Properties PROPERTIES = new Properties();
    private static DataSource dataSource;

    private static Logger LOGGER = LoggerFactory.getLogger(JdbcUtil.class);
    private static final String DB_FILE_NAME = "db.properties";
    private static final String CFG_PATH = "db.cfg.path";

    //加载DBCP配置文件
    static {


        try {
            String basePath = System.getProperty(CFG_PATH);

            File file = new File(basePath, DB_FILE_NAME);
            PROPERTIES.load(new FileInputStream(file));

            dataSource = BasicDataSourceFactory.createDataSource(PROPERTIES);


        } catch (Exception e) {
            LOGGER.error("数据库连接池初始化异常！", e);
            e.printStackTrace();

        }


    }


    public static Properties loadPropertyFile(String fullFile) {
        String webRootPath = null;
        if (null == fullFile || fullFile.equals(""))
            throw new IllegalArgumentException(
                    "Properties file path can not be null : " + fullFile);
        webRootPath = JdbcUtil.class.getClassLoader().getResource("")
                .getPath();
        webRootPath = new File(webRootPath).getParent();
        InputStream inputStream = null;
        Properties p = null;
        try {
            inputStream = new FileInputStream(new File(webRootPath
                    + File.separator + fullFile));
            p = new Properties();
            p.load(inputStream);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Properties file not found: "
                    + fullFile);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Properties file can not be loading: " + fullFile);
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return p;
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();

    }


}