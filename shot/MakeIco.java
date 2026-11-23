import java.io.File;

public class MakeIco {
    public static void main(String[] args) throws Exception {
        com.toolbox.pc.AppIcon.writeIco(com.toolbox.pc.AppIcon.image(256), new File(args[0]));
        System.out.println("ico written: " + args[0]);
    }
}
