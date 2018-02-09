package main;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sl
 */
public class IdentificationCodes {

    public static int isWhite(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() > 100) {
            return 1;
        }
        return 0;
    }


    public static BufferedImage removeBackgroud(String picFile)
            throws Exception {
        BufferedImage img = ImageIO.read(new File(picFile));
        img = img.getSubimage(1, 1, img.getWidth() - 2, img.getHeight() - 2);
        int width = img.getWidth();
        int height = img.getHeight();
        double subWidth = (double) width / 4.0;
        for (int i = 0; i < 5; i++) {
            Map<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int x = (int) (1 + i * subWidth); x < (i + 1) * subWidth
                    && x < width - 1; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (isWhite(img.getRGB(x, y)) == 1)
                        continue;
                    if (map.containsKey(img.getRGB(x, y))) {
                        map.put(img.getRGB(x, y), map.get(img.getRGB(x, y)) + 1);
                    } else {
                        map.put(img.getRGB(x, y), 1);
                    }
                }
            }
            int max = 0;
            int colorMax = 0;
            for (Integer color : map.keySet()) {
                if (max < map.get(color)) {
                    max = map.get(color);
                    colorMax = color;
                }
            }
            for (int x = (int) (1 + i * subWidth); x < (i + 1) * subWidth
                    && x < width - 1; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (img.getRGB(x, y) != colorMax) {
                        img.setRGB(x, y, Color.WHITE.getRGB());
                    } else {
                        img.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }
        }
        return img;
    }


    public static void main(String[] args) {
        try {
            OkHttpClient httpClient = new OkHttpClient();

            String url = "http://login.189.cn/web/captcha?undefined&source=login&width=100&height=37&0.2707323502871577";


            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                    .addHeader("Cache-Control", "no-cache")
                    .get()
                    .build();

            Response response = httpClient.newCall(request).execute();

            File imageFile = new File(System.getProperty("user.dir") + File.separator + "doc" + File.separator + "code.png");

            FileOutputStream output = new FileOutputStream(imageFile);
            //得到网络资源的字节数组,并写入文件
            output.write(response.body().bytes());
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
