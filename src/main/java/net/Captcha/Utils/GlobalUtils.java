package net.Captcha.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.Captcha.common.Image;
import net.Captcha.config.AppConfig;

public class GlobalUtils {
    private static final Logger logger = LoggerFactory.getLogger(GlobalUtils.class);
    public static final File img_Path;
    static {
        img_Path = AppConfig.current().imageRoot().toFile();
        if ((!img_Path.exists() && !img_Path.mkdirs()) || !img_Path.isDirectory()) {
            throw new IllegalStateException("Unable to initialize IMAGE_ROOT");
        }
    }

    public static boolean writeToLocal(Image img, String message) {
        try {
            File img_path = GlobalUtils.img_Path;
            String[] split = message.split("-");
            // 数组越界防护
            if (split.length < 3) {
                logger.error("writeToLocal: message格式错误: {}", message);
                return false;
            }
            if (!isSafeComponent(split[0]) || !isSafeComponent(split[1]) || !isSafeComponent(split[2])) {
                logger.error("writeToLocal: 拒绝不安全的路径组件");
                return false;
            }

            File user_path = new File(img_path, split[0]);
            if(!user_path.exists() && !user_path.mkdirs()) {
                logger.error("writeToLocal: 创建用户目录失败: {}", user_path.getPath());
                return false;
            }

            File date_path = new File(user_path, split[1]);
            if(!date_path.exists() && !date_path.mkdirs()) {
                logger.error("writeToLocal: 创建日期目录失败: {}", date_path.getPath());
                return false;
            }

            //全部改为.jpeg存储
            File oneI = new File(date_path, split[2] + ".jpeg");

            // 数据验证
            byte[] data = img.getData();
            if (data == null || data.length == 0) {
                logger.error("writeToLocal: 图片数据为空");
                return false;
            }

            try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(oneI))){
                bos.write(data, 0, data.length);
                bos.flush(); // 确保数据写入磁盘
            } catch (IOException e) {
                logger.error("writeToLocal: 保存图片失败: {}", oneI.getPath(), e);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("writeToLocal: 保存图片异常", e);
            return false;
        }
    }

    public static void deleteFromLocal(Image img, String message) {
        try {
            File img_path = GlobalUtils.img_Path;
            String[] split = message.split("-");
            // 数组越界防护
            if (split.length < 3) {
                logger.warn("deleteFromLocal: message格式错误: {}", message);
                return;
            }
            if (!isSafeComponent(split[0]) || !isSafeComponent(split[1]) || !isSafeComponent(split[2])) {
                logger.warn("deleteFromLocal: 拒绝不安全的路径组件");
                return;
            }
            File user_path = new File(img_path, split[0]);
            File date_path = new File(user_path, split[1]);
            File oneI = new File(date_path, split[2] + ".jpeg"); // 统一使用.jpeg
            if(oneI.exists() && !oneI.delete()) {
                logger.warn("deleteFromLocal: 删除文件失败: {}", oneI.getPath());
            }
        } catch (Exception e) {
            logger.error("deleteFromLocal: 删除文件异常", e);
        }
    }

    private static boolean isSafeComponent(String value) {
        return value != null && value.matches("[A-Za-z0-9.]+") && !value.contains("..");
    }
}
