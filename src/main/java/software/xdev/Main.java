package software.xdev;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class Main
{
	private static boolean failures = false;
	private static List<String> patched = new ArrayList<>();
	
	public static void main(final String[] args)
	{
		System.out.println("XDEV5 JAR-Library Patcher");
		System.out.println("Fixes JARs so that XDEV5 can use them");
		
		File dir = Paths.get("").toAbsolutePath().toFile();
		
		final Options options = new Options();
		
		final Option input = new Option("d", "directory", true, "Directory with libs to patch");
		options.addOption(input);
		
		final CommandLineParser parser = new DefaultParser();
		final HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;
		
		try
		{
			cmd = parser.parse(options, args);
		}
		catch(final ParseException e)
		{
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			
			System.exit(1);
			return;
		}
		
		final String cmdDirectory = cmd.getOptionValue("directory");
		if(cmdDirectory != null)
		{
			dir = new File(cmdDirectory);
		}
		
		System.out.println("Working with directory '" + dir + "'");
		
		if(!dir.exists() || !dir.isDirectory())
		{
			System.out.println("Directory is invalid");
			System.exit(1);
			return;
		}
		
		final File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
		
		for(final File f : files)
		{
			if(!f.isFile())
			{
				System.out.println("Skipping '" + f + "'");
				continue;
			}
			
			try
			{
				patchJarFile(f);
			}
			catch(final Exception e)
			{
				failures = true;
				
				System.out.println("Failure!");
				System.err.println("Failed to patch JAR '" + f + "': " + e.getMessage());
				e.printStackTrace();
			}
			System.out.println();
		}
		
		if(!patched.isEmpty())
		{
			System.out.println("#### SUMMARY ####");
			for(final String p : patched)
			{
				System.out.println(p);
			}
		}
		
		if(failures)
		{
			System.out.println("There are failures!");
			System.err.println("There are failures!");
		}
	}
	
	private static void patchJarFile(final File file) throws Exception
	{
		System.out.println("Processing '" + file + "'");
		
		List<String> entriesToRemove = null;
		try(ZipFile zipFile = new ZipFile(file))
		{
			entriesToRemove =
				enumerationAsStream(zipFile.entries())
					.filter(ze -> ze.getName().endsWith("module-info.class"))
					.map(ZipEntry::getName)
					.collect(Collectors.toList());
		}
		
		if(entriesToRemove == null || entriesToRemove.isEmpty())
		{
			System.out.println("Nothing to patch");
			return;
		}
		
		System.out.println("Found files for patching");
		
		final Map<String, String> zipProperties = new HashMap<>();
		zipProperties.put("create", "false");
		
		final String uriStr = "jar:" + file.toPath().toUri().toString();
		
		System.out.println("Accessing URI='" + uriStr + "'");
		
		final URI zipDisk = URI.create(uriStr);
		
		try(FileSystem zipFs = FileSystems.newFileSystem(zipDisk, zipProperties))
		{
			for(final String removePath : entriesToRemove)
			{
				try
				{
					final Path pathToDelete = zipFs.getPath(removePath);
					
					if(Files.isDirectory(pathToDelete))
					{
						continue;
					}
					
					System.out.println("Removing '" + removePath + "'");
					
					Files.delete(pathToDelete);
					
					patched.add("[DEL]"+file + ":" + removePath);
				}
				catch(final Exception e)
				{
					failures = true;
					
					System.out.println("Failure!");
					System.err.println("Failed to patch Entry '" + removePath + "': " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("Patched successfully");
	}
	
	public static <T> Stream<T> enumerationAsStream(final Enumeration<T> e)
	{
		return StreamSupport.stream(
			new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED)
			{
				@Override
				public boolean tryAdvance(final Consumer<? super T> action)
				{
					if(e.hasMoreElements())
					{
						action.accept(e.nextElement());
						return true;
					}
					return false;
				}
				
				@Override
				public void forEachRemaining(final Consumer<? super T> action)
				{
					while(e.hasMoreElements())
					{
						action.accept(e.nextElement());
					}
				}
			},
			false);
	}
}
