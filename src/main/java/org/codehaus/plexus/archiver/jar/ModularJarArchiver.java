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

import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.archiver.util.ResourceUtils;
import org.codehaus.plexus.archiver.zip.ConcurrentJarCreator;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

public class ModularJarArchiver
    extends JarArchiver
{
    private String moduleMainClass;

    private String moduleVersion;

    private boolean moduleDescriptorFound;

    private Path tempDir;

    public String getModuleMainClass()
    {
        return moduleMainClass;
    }

    public void setModuleMainClass(String moduleMainClass)
    {
        this.moduleMainClass = moduleMainClass;
    }

    public String getModuleVersion()
    {
        return moduleVersion;
    }

    public void setModuleVersion(String moduleVersion)
    {
        this.moduleVersion = moduleVersion;
    }

    @Override
    protected void zipFile(InputStreamSupplier is, ConcurrentJarCreator zOut, String vPath, long lastModified, File fromArchive, int mode, String symlinkDestination, boolean addInParallel) throws IOException, ArchiverException {
        if ( isModuleDescriptor( vPath ) )
        {
            // TODO check for symlinks
            moduleDescriptorFound = true;

            if ( tempDir == null)
            {
                tempDir = Files.createTempDirectory( "plexus-archiver-modular-jar" );
                tempDir.toFile().deleteOnExit();
            }

            File destFile = tempDir.resolve( vPath ).toFile();
            destFile.deleteOnExit();
            ResourceUtils.copyFile( is.get(), destFile );
            ArchiveEntryUtils.chmod( destFile,  mode );
            destFile.setLastModified( lastModified == PlexusIoResource.UNKNOWN_MODIFICATION_DATE
                                                      ? System.currentTimeMillis()
                                                      : lastModified );
        }
        else
        {
            super.zipFile( is, zOut, vPath, lastModified, fromArchive, mode, symlinkDestination, addInParallel );
        }
    }

    @Override
    protected void close()
        throws IOException
    {
        super.close();

        if ( !moduleDescriptorFound )
        {
            // TODO Throw exception?
            return;
        }

        // TODO Handle no jar tool available
        ToolProvider jarTool = ToolProvider.findFirst( "jar" ).get();
        // TODO Check the out and err to use
        int result = jarTool.run( System.out, System.err, getJarToolArguments() );

        if (result != 0)
        {
            //TODO Better exception
            throw new RuntimeException();
        }
    }

    private boolean isModuleDescriptor(String path)
    {
        // TODO Make it work with multi-version files
        return path.equals("module-info.class");
    }

    private String[] getJarToolArguments()
    {
        List<String> args = new ArrayList<>();

        args.add( "--update" );
        args.add( "--file" );
        args.add( getDestFile().getAbsolutePath() );

        if ( moduleMainClass != null )
        {
            args.add( "--main-class" );
            args.add( moduleMainClass );
        }

        if ( moduleVersion != null )
        {
            args.add( "--module-version" );
            args.add( moduleVersion );
        }

        args.add( "-C" );
        args.add( tempDir.toFile().getAbsolutePath() );
        args.add( "." );

        return args.toArray(new String[]{});
    }

}
