
# xdev5-lib-jar-patcher
A little tool that removes `module-info.class` from jar files, so you can use the jar files with XDEV5.

## Quickstart guide:

Save the `xdev5-lib-jar-patcher.jar` file to your system.

Open the command prompt from your Windows system or a terminal on MacOS or Linux.

Navigate via terminal to the downloaded `xdev5-lib-jar-patcher.jar` file.

Now enter the following command into your terminal.

`java -jar xdev5-lib-jar-patcher.jar -d "C:\libs"`

Parameters:
-d: Enter the path to your .jar files here.

You should end up with a similar message in the terminal:

    #### SUMMARY #### 
    [DEL]C:\libs\jackson-core-2.10.5.jar:module-info.class 
    [DEL]C:\libs\jaxb-api-2.4.0-b180830.0359.jar:module-info.class

