import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by 王麟东 on 2017/7/11 0011.
 */
public class Crawler {
    private BasicCookieStore cookieStore;
    private CloseableHttpClient httpclient;
    private String username;
    private String password;
    private static String educationalUrl = "http://210.26.111.34";
    private static String loginUrl = "http://210.26.111.34/mlogin.do";

    /**
     * 构造方法，获得Cookie并登录
     * @param username  账号
     * @param password  密码
     */
    public Crawler(String username, String password) {
        this.username = username;
        this.password = password;

        this.cookieStore = new BasicCookieStore();
        this.httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();

        try {
            //获取cookie
            HttpGet getCookie = new HttpGet(educationalUrl);
            CloseableHttpResponse response1 = httpclient.execute(getCookie);
            response1.close();
            //登录
            HttpUriRequest postLogin = RequestBuilder.post().setUri(new URI(loginUrl))
                    .addParameter("utype", "S")
                    .addParameter("ucode", this.username)
                    .addParameter("pwd", this.password)
                    .addParameter("rember", "true")
                    .build();
            CloseableHttpResponse response2 = httpclient.execute(postLogin);
            response2.close();

        }catch (URISyntaxException | IOException e){
            System.out.println("登录失败");
            System.exit(1);
        }
    }

    /**
     * 获得外语等级考试成绩
     * @return  JSON
     */
    public JsonArray getCTEMark() {
        JsonArray ret = new JsonArray();
        try {
            String cteUrl = "http://210.26.111.34/result/stqryFResult/view.do";
            HttpUriRequest postCTEMark = RequestBuilder.post().setUri(new URI(cteUrl))
                    .build();
            CloseableHttpResponse response = httpclient.execute(postCTEMark);
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity);
            EntityUtils.consume(entity);

            Gson gson = new Gson();
            JsonObject jo = gson.fromJson(json, JsonObject.class);

            JsonArray ja = jo.getAsJsonArray("rows");

            for (JsonElement je : ja) {
                JsonObject ctemark = new JsonObject();
                for (Map.Entry<String, JsonElement> e : je.getAsJsonObject().entrySet()) {
                    switch (e.getKey()) {
                        //时间
                        case ("FYM"):
                            ctemark.addProperty("date", e.getValue().getAsString());
                            break;
                        //语种
                        case ("FDESC"):
                            ctemark.addProperty("subject", e.getValue().getAsString());
                            break;
                        //等级
                        case ("FLEVEL"):
                            ctemark.addProperty("level", e.getValue().getAsString());
                            break;
                        //成绩
                        case ("FE_FLR_CORDE"):
                            ctemark.addProperty("mark", e.getValue().getAsInt());
                            break;
                    }
                }
                ret.add(ctemark);
            }
        } catch (com.google.gson.JsonSyntaxException g) {
            System.out.println("获取失败，请检查账号密码");
            System.exit(1);
        } catch (IOException | URISyntaxException e) {
            System.out.println("获取失败，请检查网络");
            System.exit(1);
        }

        return ret;
    }

    /**
     * 获得课程表
     * @return  JSON
     */
    public JsonArray getCoursTable(){
        JsonArray ret = new JsonArray();
        try {
            String courseTableUrl = "http://210.26.111.34/course/cuTable/view.do";
            HttpUriRequest postCourseTable = RequestBuilder.post().setUri(new URI(courseTableUrl))
                    .addParameter("flag", "1")
                    .addParameter("id", "0")
                    .build();

            CloseableHttpResponse response = httpclient.execute(postCourseTable);
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity);
            EntityUtils.consume(entity);

            Gson gson = new Gson();
            JsonArray jo = gson.fromJson(json, JsonArray.class);

            for (JsonElement j : jo) {
                int section = 0;
                switch (j.getAsJsonObject().get("section").getAsString()) {
                    case "1-2节":
                        section = 1;
                        break;
                    case "3-4节":
                        section = 2;
                        break;
                    case "5-6节":
                        section = 3;
                        break;
                    case "7-8节":
                        section = 4;
                        break;
                    case "9-10节":
                        section = 5;
                        break;
                    default:
                        section = 0;
                        break;
                }

                for (Map.Entry<String, JsonElement> entry : j.getAsJsonObject().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue().getAsString();

                    if (!key.equals("section")) {
                        int week = 0;
                        int isSingle = 0;

                        switch (key) {
                            case "week1":
                                week = 1;
                                break;
                            case "week2":
                                week = 2;
                                break;
                            case "week3":
                                week = 3;
                                break;
                            case "week4":
                                week = 4;
                                break;
                            case "week5":
                                week = 5;
                                break;
                            default:
                                week = 0;
                                break;
                        }

                        if (value.indexOf("单：</font>") != -1 && value.indexOf("双：</font>") != -1) {
                            //两门课
                            String course1 = value.substring(value.indexOf("单：</font>") +
                                            "单：</font>".length(),
                                    value.indexOf("<br><font"));
                            String course2 = value.substring(value.indexOf("双：</font>") +
                                    "双：</font>".length());

                            ret.add(getCourseJson(course1, week, section, 1));
                            ret.add(getCourseJson(course2, week, section, 2));
                        } else {
                            //一门课
                            if (value.indexOf("单：</font>") != -1) {
                                isSingle = 1;
                            } else if (value.indexOf("双：</font>") != -1) {
                                isSingle = 2;
                            }

                            String coursejson = null;
                            if (isSingle == 0) {
                                coursejson = value;
                            } else if (isSingle == 1) {
                                coursejson = value.substring(value.indexOf("单：</font>") +
                                                "单：</font>".length(),
                                        value.length());
                            } else if (isSingle == 2) {
                                coursejson = value.substring(value.indexOf("双：</font>") +
                                                "双：</font>".length(),
                                        value.length());
                            }
                            ret.add(getCourseJson(coursejson, week, section, isSingle));
                        }
                    }
                }
            }
        } catch (com.google.gson.JsonSyntaxException g) {
            System.out.println("获取失败，请检查账号密码");
            System.exit(1);
        } catch (IOException | URISyntaxException e) {
            System.out.println("获取失败，请检查网络");
            System.exit(1);
        }

        return ret;
    }

    /**
     * 辅助方法，传入格式化后字符串，返回JsonObject
     * @param course
     * @param week
     * @param section
     * @param isSingle
     * @return
     */
    private static JsonObject getCourseJson(String course, int week, int section, int isSingle){
        try {
            int beginweek = 1;
            int endweek = 18;
            String name = course.substring(0, course.indexOf("<br>教师"));;
            String teacher = course.substring(course.indexOf("教师：") + "教师：".length(),
                    course.indexOf("<br>教室"));;
            String place = course.substring(course.indexOf("教室：") + "教室：".length(), course.length());

            if(teacher.indexOf("[") != -1 && teacher.indexOf("]") != -1){
                beginweek = Integer.parseInt(teacher.substring(teacher.indexOf("[") + "[".length(),
                        teacher.indexOf("-")));
                endweek = Integer.parseInt(teacher.substring(teacher.indexOf("-") + "-".length(),
                        teacher.indexOf("]")));
                teacher = teacher.substring(0, teacher.indexOf("["));
            }

            JsonObject jo1 = new JsonObject();
            jo1.addProperty("name", name);
            jo1.addProperty("teacher", teacher);
            jo1.addProperty("beginweek", beginweek);
            jo1.addProperty("endweek", endweek);
            jo1.addProperty("place", place);
            jo1.addProperty("isSingle", isSingle);
            jo1.addProperty("week", week);
            jo1.addProperty("section", section);
            return jo1;

        }catch (NullPointerException e){
            return null;
        }

    }

    /**
     * 获得考试成绩
     * @return
     */
    public JsonArray getExamMark() {
        JsonArray ret = new JsonArray();

        try {
            String getExamMark = "http://210.26.111.34/result/stqryResult/view.do";
            HttpUriRequest postExamMark = RequestBuilder.post().setUri(new URI(getExamMark))
                    .build();

            CloseableHttpResponse response = httpclient.execute(postExamMark);
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity);
            EntityUtils.consume(entity);

            Gson gson = new Gson();

            JsonObject jo = gson.fromJson(json, JsonObject.class);
            JsonArray ja = jo.getAsJsonArray("rows");

            for (JsonElement e : ja) {
                JsonObject cuMark = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : e.getAsJsonObject().entrySet()) {
                    switch (entry.getKey()) {
                        //课程名
                        case ("FE_CU_NAME"):
                            cuMark.addProperty("name", entry.getValue().getAsString());
                            break;
                        //学期
                        case ("FE_CU_YEAR"):
                            cuMark.addProperty("date", entry.getValue().getAsString());
                            break;
                        //学分
                        case ("FE_CU_CREDIT"):
                            cuMark.addProperty("credit", entry.getValue().getAsString());
                            break;
                        //平时成绩
                        case ("FE_SR_USUAL1"):
                            cuMark.addProperty("usual", entry.getValue().getAsString());
                            break;
                        //期末成绩
                        case ("FE_SR_FINAL"):
                            cuMark.addProperty("final", entry.getValue().getAsString());
                            break;
                        //总评成绩
                        case ("FE_SR_TOTAL"):
                            cuMark.addProperty("total", entry.getValue().getAsString());
                            break;
                        //补考成绩
                        case ("FE_SR_REEXAM"):
                            cuMark.addProperty("reexam", entry.getValue().getAsString());
                            break;
                        //最终成绩
                        case ("FE_SR_CORDE"):
                            cuMark.addProperty("corde", entry.getValue().getAsString());
                            break;
                        //成绩类型
                        case ("FE_SR_FLAG"):
                            cuMark.addProperty("markType", entry.getValue().getAsString());
                            break;
                        //课程类型
                        case ("FE_CU_TYPE"):
                            cuMark.addProperty("courseType", entry.getValue().getAsString());
                            break;
                    }
                }
                ret.add(cuMark);
            }

        } catch (com.google.gson.JsonSyntaxException g) {
            System.out.println("获取失败，请检查账号密码");
            System.exit(1);
        } catch (IOException | URISyntaxException e) {
            System.out.println("获取失败，请检查网络");
            System.exit(1);
        }

        return ret;
    }
}
