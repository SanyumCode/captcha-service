package net.Captcha.common;

import java.io.Serializable;
import java.util.Date;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;
@Entity("Images")
@Indexes({
        @Index(fields = @Field("uid") ),
        @Index(fields = @Field("usid")),
        @Index(fields = @Field("st"))
})

public class Image implements Serializable{

    @Override
    public String toString() {
        return "Image [id=" + id + ", coded=" + coded + ", uid=" + uid + ", type=" + type + ", userId=" + userId
                + ", storeTime=" + storeTime + ", completeTime=" + completeTime + ", status=" + status
                + ", dataLength=" + (data == null ? 0 : data.length) + "]";
    }
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    private String coded;

    private String uid;

    private ImgType type;

    @Property("usid")
    private int userId;

    @Property("stt")
    private Date storeTime;

    @Property("cmt")
    private Date completeTime;

    //I 初始化状态 F识别失败 S 完成识别
    @Property("st")
    private String status;

    @Transient
    private byte[] data;

    public void setUID(String uid) {
        this.uid = uid;
    }
    public String getUID() {
        return uid != null ? uid : "";
    }
    public String getCoded() {
        return coded;
    }
    public void setCoded(String coded) {
        this.coded = coded;
    }
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
    public ImgType getType() {
        return type;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setType(ImgType type) {
        this.type = type;
    }
    public Date getStoreTime() {
        return storeTime;
    }
    public void setStoreTime(Date storeTime) {
        this.storeTime = storeTime;
    }
    public Date getCompleteTime() {
        return completeTime;
    }
    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }


}
