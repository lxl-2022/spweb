
import com.immoc.bilibiliapplication;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;


@SpringBootTest(classes = {bilibiliapplication.class})
public class test1 {

    @Test
    public void Mytest(){
        //1创建Jedis对象
        Jedis jedis = new Jedis("192.168.61.150",6379);
        String ping = jedis.ping();
        System.out.println("连接成功");
    }
}
