package net.vansencool.lsyaml;

import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.ParseIssue;
import net.vansencool.lsyaml.parser.ParseOptions;
import net.vansencool.lsyaml.parser.ParseResult;

public class ManualTest {

    public static void main(String[] args) {
        String yaml = """
            # Database configuration
                    database:
              host: localhost  # primary host
              port: 5432
            
            
            
            
                # Connection pool settings
                pool: # At pool settings
                m          in: 5
                max: 20
            """;

        ParseResult result = LSYAML.parseDetailed(yaml, ParseOptions.defaults());

        if (!result.isSuccess()) {
            for (ParseIssue issue : result.getIssues()) {
                System.out.println(issue.format());
            }
        }
    }
}
