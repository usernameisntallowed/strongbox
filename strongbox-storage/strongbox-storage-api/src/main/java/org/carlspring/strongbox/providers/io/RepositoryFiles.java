package org.carlspring.strongbox.providers.io;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.domain.RemoteArtifactEntry;

public abstract class RepositoryFiles
{

    public static Boolean isChecksum(RepositoryPath path)
        throws IOException
    {
        return (Boolean) Files.getAttribute(path, formatAttributes(RepositoryFileAttributeType.CHECKSUM));
    }

    public static Boolean isMetadata(RepositoryPath path)
        throws IOException
    {
        return (Boolean) Files.getAttribute(path, formatAttributes(RepositoryFileAttributeType.METADATA));
    }

    public static Boolean isTrash(RepositoryPath path)
        throws IOException
    {
        return (Boolean) Files.getAttribute(path, formatAttributes(RepositoryFileAttributeType.TRASH));
    }

    public static Boolean isTemp(RepositoryPath path)
        throws IOException
    {
        return (Boolean) Files.getAttribute(path, formatAttributes(RepositoryFileAttributeType.TEMP));
    }
    
    public static Boolean isIndex(RepositoryPath path)
        throws IOException
    {
        return (Boolean) Files.getAttribute(path, formatAttributes(RepositoryFileAttributeType.INDEX));
    }
    
    public static Boolean isArtifact(RepositoryPath path)
        throws IOException
    {
        return (Boolean) Files.getAttribute(path, formatAttributes(RepositoryFileAttributeType.ARTIFACT));
    }

    public static ArtifactCoordinates readCoordinates(RepositoryPath path)
        throws IOException
    {
        return (ArtifactCoordinates) Files.getAttribute(path,
                                                        formatAttributes(RepositoryFileAttributeType.COORDINATES));
    }

    public static URL readResourceUrl(RepositoryPath path)
        throws IOException
    {
        return (URL) Files.getAttribute(path, formatAttributes(RepositoryFileAttributeType.RESOURCE_URL));
    }
    
    public static String formatAttributes(RepositoryFileAttributeType... attributeTypes)
    {
        if (attributeTypes == null)
        {
            return "strongbox:*";
        }
        StringJoiner sj = new StringJoiner(",");
        for (RepositoryFileAttributeType repositoryFileAttributeType : attributeTypes)
        {
            sj.add(repositoryFileAttributeType.getName());
        }
        return String.format("strongbox:%s", sj.toString());
    }

    public static Set<RepositoryFileAttributeType> parseAttributes(String attributes)
    {
        if (attributes == null)
        {
            return Collections.emptySet();
        }
        String attributesLocal = attributes.replace("strongbox:", "").trim();
        if (attributesLocal.equals("*"))
        {
            return Arrays.stream(RepositoryFileAttributeType.values())
                         .collect(Collectors.toSet());
        }
        return Arrays.stream(attributesLocal.split(","))
                     .map(e -> RepositoryFileAttributeType.of(e))
                     .collect(Collectors.toSet());
    }

    public static URI relativizeUri(RepositoryPath p)
        throws IOException
    {
        URI result = p.getFileSystem().getRootDirectory().toUri();

        if (isTrash(p))
        {
            result = result.resolve(RepositoryFileSystem.TRASH);
        }
        else if (isTemp(p))
        {
            result = result.resolve(RepositoryFileSystem.TEMP);
        }

        return result.relativize(p.toUri());
    }

    public static void undelete(RepositoryPath p)
        throws IOException
    {
        p.getFileSystem().provider().undelete(p);
    }

    public static void permanent(RepositoryPath p)
        throws IOException
    {
        p.getFileSystem().provider().moveFromTemporaryDirectory(p);
    }

    public static RepositoryPath temporary(RepositoryPath p)
        throws IOException
    {
        return p.getFileSystem().provider().getTempPath(p);
    }
    
    public static RepositoryPath trash(RepositoryPath p)
        throws IOException
    {
        return p.getFileSystem().provider().getTrashPath(p);
    }

    public static String stringValue(RepositoryPath p)
            throws IOException
    {
        if (p.path != null)
        {
            return p.path;
        }
        if (!p.isAbsolute())
        {
            return p.path = p.toString();
        }

        return p.path = relativizeUri(p).toString();
    }
    
    public static URI resolveResource(RepositoryPath p)
        throws IOException
    {
        if (RepositoryFiles.isArtifact(p))
        {
            ArtifactCoordinates c = RepositoryFiles.readCoordinates(p);
            
            return c.toResource();
        }
        
        return relativizeUri(p);
    }

    public static boolean artifactExists(RepositoryPath repositoryPath)
        throws IOException
    {
        return !artifactDoesNotExist(repositoryPath);
    }
    
    public static boolean artifactDoesNotExist(RepositoryPath repositoryPath)
        throws IOException
    {
        ArtifactEntry e = repositoryPath.getArtifactEntry();
        if (RepositoryFiles.isArtifact(repositoryPath))
        {
            return e == null || e instanceof RemoteArtifactEntry && !((RemoteArtifactEntry) e).getIsCached();
        }
        else
        {
            return !Files.exists(repositoryPath);
        }
    }

}
