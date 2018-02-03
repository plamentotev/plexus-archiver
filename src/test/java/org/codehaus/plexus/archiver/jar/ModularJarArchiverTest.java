/**
 *
 * Copyright 2018 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.jar;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.net.URL;

public class ModularJarArchiverTest
    extends TestCase
{

    public void testModularJar()
            throws IOException
    {
        File jarFile = new File( "target/output/modular.jar" );
        jarFile.delete();

        // verify that the original module declaration does not have main class and version set
        ModuleDescriptor originalModuleDescriptor = ModuleDescriptor.read(
            new FileInputStream( "src/test/resources/java-module/module-info.class" ) );
        assertFalse( originalModuleDescriptor.mainClass().isPresent() );
        assertFalse( originalModuleDescriptor.version().isPresent() );

        ModularJarArchiver archiver = new ModularJarArchiver();
        archiver.setDestFile( jarFile );
        archiver.addDirectory( new File( "src/test/resources/java-module" ) );
        archiver.setModuleVersion( "1.0.0" );
        archiver.setModuleMainClass( "com.example.app.Main" );

        archiver.createArchive();

        // verify that the resulting modular jar has the proper version and main class set
        ModuleDescriptor resultingModuleDescriptor = ModuleDescriptor.read(
                new URL( "jar:file:target/output/modular.jar!/module-info.class" ).openStream() );
        assertEquals( "1.0.0", resultingModuleDescriptor.version().get().toString() );
        assertEquals( "com.example.app.Main", resultingModuleDescriptor.mainClass().get() );
    }

}
