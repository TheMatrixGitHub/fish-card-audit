package entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;
import java.util.Objects;

/**
 * @author sl
 */
public class TrafficInternetRecords {
    private Integer id;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date start_time;
    private String business_name;
    private String url;

    @Override
    public String toString() {
        return "TrafficInternetRecords{" +
                "id=" + id +
                ", start_time=" + start_time +
                ", business_name='" + business_name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrafficInternetRecords that = (TrafficInternetRecords) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(start_time, that.start_time) &&
                Objects.equals(business_name, that.business_name) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, start_time, business_name, url);
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

    public String getBusiness_name() {
        return business_name;
    }

    public void setBusiness_name(String business_name) {
        this.business_name = business_name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}