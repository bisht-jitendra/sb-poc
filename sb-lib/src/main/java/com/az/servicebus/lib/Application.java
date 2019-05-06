package com.az.servicebus.lib;

import com.az.servicebus.lib.config.ServiceBusConfig;
import com.az.servicebus.lib.services.ServiceBusManager;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

public class Application
{

    public static void main(String[] args)
    {
        new Application().runApplication(args);
    }

    private void runApplication(String[] args)
    {
        ServiceBusConfig config = new ServiceBusConfig();
        CmdLineParser parser = new CmdLineParser(config);
        try
        {
            parser.parseArgument(args);
            ServiceBusManager manager = ServiceBusManager.getInstance(config);
            manager.run();
        } catch (CmdLineException ex)
        {
            System.err.println(ex.getMessage());
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("  Example: java SampleMain" + parser.printExample(ALL));
        } catch (Exception e)
        {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }
}
