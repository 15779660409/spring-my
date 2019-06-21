/**
 * @author kanghaijun
 * @create 2019/6/21
 * @describe
 */
public class Test {

    @org.junit.Test
    public void testFirstLowerCase(){
        String s = "UserController";
        char[] chars = s.toCharArray();
        chars[0] += 32;
        String s1 = String.valueOf(chars);
        System.out.println(s1);
    }

}
