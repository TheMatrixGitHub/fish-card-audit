package entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;
import java.util.Objects;

/**
 * @author sl
 */
public class DetailedFlow {
    private Integer id;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date start_time;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date end_time;
    private String flow;
    private String type;
    private String belong_area;
    private String key_type;

    @Override
    public String toString() {
        return "DetailedFlow{" +
                "id=" + id +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                ", flow='" + flow + '\'' +
                ", type='" + type + '\'' +
                ", belong_area='" + belong_area + '\'' +
                ", key_type='" + key_type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetailedFlow that = (DetailedFlow) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(start_time, that.start_time) &&
                Objects.equals(end_time, that.end_time) &&
                Objects.equals(flow, that.flow) &&
                Objects.equals(type, that.type) &&
                Objects.equals(belong_area, that.belong_area) &&
                Objects.equals(key_type, that.key_type);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, start_time, end_time, flow, type, belong_area, key_type);
    }

    public Integer getId() {

        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getStart_time() {
        return start_time;
    }

    public void setStart_time(Date start_time) {
        this.start_time = start_time;
    }

    public Date getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Date end_time) {
        this.end_time = end_time;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBelong_area() {
        return belong_area;
    }

    public void setBelong_area(String belong_area) {
        this.belong_area = belong_area;
    }

    public String getKey_type() {
        return key_type;
    }

    public void setKey_type(String key_type) {
        this.key_type = key_type;
    }
}