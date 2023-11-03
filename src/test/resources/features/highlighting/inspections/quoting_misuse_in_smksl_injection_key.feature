Feature: Inspection warns about key quoting misuse in snakemake string language injections.
  Scenario Outline: Quotes misuse
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like>:
        shell: <quote>/foo/{<content>}.fq<quote>
    """
    And SmkSLQuotingMisuseInGetAccessorInspection inspection is enabled
    Then I expect inspection warning on <<problem>> with message
      """
      Key quoting isn't expected in Snakemake string language get accessor.
      """
    When I check highlighting warnings
    When I put the caret at <problem>
    And I invoke quick fix Unquote key and see text:
    """
    <rule_like>:
        shell: <quote>/foo/{<content_fixed>}.fq<quote>
    """
    Examples:
      | rule_like  | quote | content               | problem | content_fixed        |
      | rule       | "     | config[boo']          | boo'    | config[boo]          |
      | rule       | "     | config['boo]          | 'boo    | config[boo]          |
      | rule       | "     | config[\"boo\"]       | \"boo\" | config[boo]          |
      | rule       | "     | config[\"boo]         | \"boo   | config[boo]          |
      | rule       | "     | config[boo\"]         | boo\"   | config[boo]          |
      | rule       | '     | config["boo"]         | "boo"   | config[boo]          |
      | rule       | '     | config["boo]          | "boo    | config[boo]          |
      | rule       | '     | config[boo"]          | boo"    | config[boo]          |
      | rule       | '     | config[\'boo\']       | \'boo\' | config[boo]          |
      | rule       | '     | config[\'boo]         | \'boo   | config[boo]          |
      | rule       | '     | config[boo\']         | boo\'   | config[boo]          |
      | checkpoint | "     | config['boo']         | 'boo'   | config[boo]          |
      | rule       | "     | zoo.config[boo']      | boo'    | zoo.config[boo]      |
      | rule       | "     | zoo[too].config[boo'] | boo'    | zoo[too].config[boo] |

  Scenario: No error if no quotes misuse
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      rule:
          shell: "/foo/{config[boo]}.fq"
      """
      And SmkSLQuotingMisuseInGetAccessorInspection inspection is enabled
      Then I expect no inspection warnings
      When I check highlighting warnings