/**
 * Created by 王麟东 on 2017/7/11 0011.
 */
public class Test {
    @org.junit.Test
    public void test(){
        String user = "";
        String pwd = "";
        Crawler crawler = new Crawler(user, pwd);
        System.out.println(crawler.getExamMark().toString());
    }
}
