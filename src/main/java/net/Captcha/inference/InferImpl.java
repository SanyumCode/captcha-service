package net.Captcha.inference;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class InferImpl {
    private static InferImpl INSTANCE;
    private OrtSession session;
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private InferImpl(String modelPath) throws OrtException {
         OrtEnvironment env = OrtEnvironment.getEnvironment();
         session = env.createSession(modelPath);


    }
     // 初始化方法，必须先调用
    public static synchronized void init(String modelPath) throws OrtException {
        if (INSTANCE == null) {
            INSTANCE = new InferImpl(modelPath);
        }
    }

    public static InferImpl getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "InferImpl未初始化。请先调用 InferImpl.init(modelPath) 方法进行初始化。" +
                    "示例: InferImpl.init(\"path/to/model.onnx\");");
        }
        return INSTANCE;
    }

    private float[][][][] preprocessImage(String imagePath) throws Exception {
         BufferedImage image = ImageIO.read(new File(imagePath));
         if(image == null) throw new ImageReadException("图片格式非法或者图片损坏");
         // 调整大小到216x96
         BufferedImage resized = new BufferedImage(216, 96, BufferedImage.TYPE_INT_RGB);
         resized.getGraphics().drawImage(image, 0, 0, 216, 96, null);

         float[][][][] input = new float[1][3][96][216];

         // 归一化
         float[] mean = {0.485f, 0.456f, 0.406f};
         float[] std = {0.229f, 0.224f, 0.225f};

         for (int y = 0; y < 96; y++) {
             for (int x = 0; x < 216; x++) {
                 int rgb = resized.getRGB(x, y);
                 float r = ((rgb >> 16) & 0xFF) / 255.0f;
                 float g = ((rgb >> 8) & 0xFF) / 255.0f;
                 float b = (rgb & 0xFF) / 255.0f;

                 input[0][0][y][x] = (r - mean[0]) / std[0];
                 input[0][1][y][x] = (g - mean[1]) / std[1];
                 input[0][2][y][x] = (b - mean[2]) / std[2];
             }
         }

         return input;
     }

     private String decodePrediction(float[][][] output) {
         StringBuilder result = new StringBuilder();
         int prevChar = -1;

         for (int t = 0; t < output.length; t++) {
             int maxIdx = 0;
             float maxVal = output[t][0][0];

             for (int i = 1; i < output[t][0].length; i++) {
                 if (output[t][0][i] > maxVal) {
                     maxVal = output[t][0][i];
                     maxIdx = i;
                 }
             }

             if (maxIdx == CHARS.length()) { // 空白符
                 prevChar = -1;
             } else if (maxIdx != prevChar) {
                 result.append(CHARS.charAt(maxIdx));
                 prevChar = maxIdx;
             }
         }

         return result.toString();
     }

     public String predict(String imagePath) throws Exception {
         float[][][][] input = preprocessImage(imagePath);

         OnnxTensor inputTensor = null;
         try {
             inputTensor = OnnxTensor.createTensor(
                 OrtEnvironment.getEnvironment(), input);

             Map<String, OnnxTensor> inputs = new HashMap<>();
             inputs.put("input", inputTensor);

             OrtSession.Result result = session.run(inputs);
             float[][][] output = (float[][][]) result.get(0).getValue();

             return decodePrediction(output);
         } finally {
             // 资源泄漏修复：确保OnnxTensor被正确关闭
             if (inputTensor != null) {
                 try {
                     inputTensor.close();
                 } catch (Exception e) {
                     // 忽略关闭异常
                 }
             }
         }
     }

}
