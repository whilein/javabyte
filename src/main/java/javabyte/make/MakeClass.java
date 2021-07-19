/*
 *    Copyright 2021 Whilein
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package javabyte.make;

import javabyte.name.ExactName;
import javabyte.name.Name;
import javabyte.type.Version;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * @author whilein
 */
public interface MakeClass extends MakeElement {

    @NotNull MakeConstructor getStaticConstructor();

    @NotNull MakeConstructor addConstructor();

    @NotNull MakeMethod addMethod(@NotNull String name);

    @NotNull MakeField addField(@NotNull String name, @NotNull Name type);
    @NotNull MakeField addField(@NotNull String name, @NotNull Type type);

    @NotNull Version getVersion();
    @NotNull ExactName getName();

    void setSuperName(@NotNull Type type);
    void setSuperName(@NotNull Name name);

    @NotNull Name getSuperName();

    void addInterface(@NotNull Name name);
    void addInterface(@NotNull Type type);

    void setInterfaces(@NotNull Name @NotNull ... interfaces);
    void setInterfaces(@NotNull Collection<@NotNull Name> interfaces);

    void setInterfaceTypes(@NotNull Type @NotNull ... interfaces);
    void setInterfaceTypes(@NotNull Collection<@NotNull Type> interfaces);

    @NotNull List<@NotNull Name> getInterfaces();

    @NotNull Class<?> load(@NotNull ClassLoader loader);

    void writeClass(@NotNull OutputStream os) throws IOException;
    void writeClass(@NotNull File file) throws IOException;
    void writeClass(@NotNull Path path) throws IOException;

    byte @NotNull [] writeAsBytes();

}
