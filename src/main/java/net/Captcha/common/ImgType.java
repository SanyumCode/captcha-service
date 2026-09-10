package net.Captcha.common;

public enum ImgType {
    //规定图片格式的前三个字节数组
    PNG(new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E}),
    JPEG(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    GIF(new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46}),
    ERROR(new byte[]{});

    public final byte[] data;

    ImgType(byte[] data){
        this.data = data;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
         return super.toString().toLowerCase();

    }



}
