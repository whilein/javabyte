import javabyte.Javabyte;
import javabyte.name.Names;
import javabyte.type.Access;
import lombok.SneakyThrows;
import lombok.val;

import java.util.function.Supplier;

/**
 * @author whilein
 */
public final class Main {

    @SneakyThrows
    public static void main(final String[] args) {
        val example = Javabyte.make("generated.Example");

        example.addInterface(Names.exact(Supplier.class).parameterized(Integer.class));

        example.setAccess(Access.PUBLIC);
        example.setFinal(true);

        val getMethod = example.addMethod("get", Integer.class);
        getMethod.setAccess(Access.PUBLIC);
        getMethod.setOverrides(Supplier.class, "get");

        val getCode = getMethod.getBytecode();
        getCode.pushInt(123);
        getCode.callReturn();

        val exampleType = example.load(Main.class.getClassLoader());
        val exampleInstance = (Supplier<?>) exampleType.getConstructor().newInstance();

        System.out.println(exampleInstance.get());
    }

}
