import com.alphaautoleak.jnvm.Main;

public class TestLambdaMetafactory {

    public static void main(String[] args) {
        Main.main(new String[]{
                "--jar", "test/114514.jar",
                "--out", "test/114514-obf.jar",
                "--target", "x86_64-windows-gnu",
                "--anti-debug", "true",
                "--native-dir", "native",
                "--debug", "false"
        });
    }

}
